-- Massa de testes do pascoa-monolith: todos os cenarios da aplicacao em volume de estresse.
--
-- ATENCAO: apaga TODOS os dados de negocio do banco alvo (preserva usuarios/admin e configuracoes).
-- Faca backup antes se houver algo que importe:
--   docker compose exec -T postgres pg_dump -U postgres pascoa_monolith > backup.sql
--
-- Aplicacao:
--   docker compose exec -T postgres psql -U postgres -d pascoa_monolith < infra/seed/seed-massa-teste.sql
--
-- Deterministico: sem random(), tudo derivado de generate_series. Reexecutar gera o mesmo banco.

\set ON_ERROR_STOP on
\timing on

\set qtd_clientes 800
\set qtd_pedidos 5000
\set qtd_orcamentos 1500
\set qtd_mov_por_mp 1200
\set qtd_gastos 1200
\set qtd_notificacoes 2000
\set qtd_auditoria 1000

BEGIN;

-- ---------------------------------------------------------------------------
-- 0. Limpeza
-- ---------------------------------------------------------------------------
TRUNCATE TABLE
    alertas_internos, audit_log, campanha_reengajamento, checklist_qualidade,
    contas_pagar, contas_receber, despesas_fixas, despesas_variaveis,
    fichas_tecnicas_itens, fichas_tecnicas, gastos_variaveis, inspecao_qualidade,
    itens_pedido, movimentacoes_estoque, notas_cliente, notificacoes_enviadas,
    orcamento_itens, orcamentos, orcamentos_gasto, ordens_producao, pagamentos,
    password_reset_token, pedidos, pontos_fidelidade, produtos, materias_primas,
    fornecedores, templates_notificacao, clientes
RESTART IDENTITY CASCADE;

DELETE FROM usuarios WHERE login <> 'admin';

-- ---------------------------------------------------------------------------
-- 1. Usuarios: uma conta por role, inativos, com e sem 2FA (senha = a do admin)
-- ---------------------------------------------------------------------------
INSERT INTO usuarios (nome, login, senha, role, ativo, totp_secret, totp_ativado, tentativas_totp_falhas, email)
SELECT v.nome, v.login, (SELECT senha FROM usuarios WHERE login = 'admin'),
       v.role, v.ativo, v.totp_secret, v.totp_ativado, v.tentativas, v.email
FROM (VALUES
    ('Ana Financeiro',      'financeiro',  'FINANCEIRO',       true,  NULL,               false, 0, 'financeiro@pascoa.local'),
    ('Bruno Atendimento',   'atendente',   'ATENDENTE',        true,  NULL,               false, 0, 'atendente@pascoa.local'),
    ('Carla Confeiteira',   'confeiteiro', 'CONFEITEIRO',      true,  NULL,               false, 0, 'confeiteiro@pascoa.local'),
    ('Diego Qualidade',     'qualidade',   'GESTOR_QUALIDADE', true,  NULL,               false, 0, 'qualidade@pascoa.local'),
    ('Elisa Analista',      'analista',    'ANALISTA',         true,  NULL,               false, 0, 'analista@pascoa.local'),
    ('Fabio Admin 2FA',     'admin2fa',    'ADMIN',            true,  'JBSWY3DPEHPK3PXP', true,  0, 'admin2fa@pascoa.local'),
    ('Gisele Bloqueada',    'bloqueada',   'ATENDENTE',        true,  'JBSWY3DPEHPK3PXQ', true,  4, 'bloqueada@pascoa.local'),
    ('Heitor Inativo',      'inativo',     'FINANCEIRO',       false, NULL,               false, 0, 'inativo@pascoa.local'),
    ('Iris Sem Email',      'semmail',     'ATENDENTE',        true,  NULL,               false, 0, NULL),
    ('Joao Segundo Admin',  'admin2',      'ADMIN',            true,  NULL,               false, 0, 'admin2@pascoa.local')
) AS v(nome, login, role, ativo, totp_secret, totp_ativado, tentativas, email);

-- ---------------------------------------------------------------------------
-- 2. Fornecedores
-- ---------------------------------------------------------------------------
INSERT INTO fornecedores (nome, cnpj, telefone, email, observacoes, criado_em, criado_por)
SELECT 'Fornecedor ' || (ARRAY['Cacau Brasil','Doce Norte','Insumos Sul','Embala Mais','Nutri Frutas',
                               'Leite Vale','Aromas Finos','Frutas Secas','Papel Fino','Pack Express',
                               'Trufas Premium','Distribuidora Central'])[g],
       CASE WHEN g % 4 = 0 THEN NULL ELSE lpad(g::text, 2, '0') || '.345.678/0001-' || lpad(g::text, 2, '0') END,
       CASE WHEN g % 5 = 0 THEN NULL ELSE '(11) 9' || lpad((80000000 + g * 37)::text, 8, '0') END,
       CASE WHEN g % 3 = 0 THEN NULL ELSE 'contato' || g || '@fornecedor.local' END,
       CASE WHEN g % 6 = 0 THEN 'Prazo de entrega ' || (2 + g) || ' dias' END,
       now() - make_interval(days => (400 - g * 3)::int), 'admin'
FROM generate_series(1, 12) g;

-- ---------------------------------------------------------------------------
-- 3. Materias-primas (6 unidades, custo zero, sem fornecedor, custo medio divergente)
-- ---------------------------------------------------------------------------
INSERT INTO materias_primas (nome, unidade, quantidade_atual, quantidade_minima, custo_unitario,
                             custo_medio_ponderado, data_ultima_compra, fornecedor_preferencial_id,
                             criado_em, criado_por)
SELECT v.nome, v.unidade, 0, v.minima, v.custo,
       CASE WHEN v.ord % 7 = 0 THEN round(v.custo * 1.18, 4) ELSE v.custo END,
       CURRENT_DATE - (v.ord * 3), CASE WHEN v.ord % 9 = 0 THEN NULL ELSE 1 + (v.ord % 12) END,
       now() - make_interval(days => (500 - v.ord * 4)::int), 'admin'
FROM (VALUES
    ( 1, 'Chocolate ao Leite',        'KG',  20.000,  42.0000),
    ( 2, 'Chocolate Meio Amargo',     'KG',  15.000,  48.5000),
    ( 3, 'Chocolate Branco',          'KG',  12.000,  51.0000),
    ( 4, 'Chocolate Diet',            'KG',   8.000,  67.9000),
    ( 5, 'Chocolate Vegano',          'KG',   6.000,  72.4000),
    ( 6, 'Recheio Trufa Tradicional', 'KG',   5.000,  68.0000),
    ( 7, 'Recheio Brigadeiro',        'KG',   5.000,  39.9000),
    ( 8, 'Recheio Maracuja',          'KG',   4.000,  44.2000),
    ( 9, 'Doce de Leite',             'KG',   4.000,  33.7000),
    (10, 'Creme de Avela',            'KG',   3.000,  89.9000),
    (11, 'Castanha de Caju',          'KG',   2.000, 119.0000),
    (12, 'Amendoim Torrado',          'KG',   3.000,  27.5000),
    (13, 'Leite Condensado',          'L',   10.000,  12.9000),
    (14, 'Creme de Leite',            'L',    8.000,  10.4000),
    (15, 'Essencia de Baunilha',      'ML', 500.000,   0.3500),
    (16, 'Corante Alimenticio',       'ML', 300.000,   0.2800),
    (17, 'Acucar Cristal',            'KG',  25.000,   5.2000),
    (18, 'Adocante Culinario',        'G',  800.000,   0.1900),
    (19, 'Cacau em Po',               'G', 2000.000,   0.0800),
    (20, 'Embalagem Ovo Pequeno',     'UN',  50.000,   2.1000),
    (21, 'Embalagem Ovo Grande',      'UN',  40.000,   3.5000),
    (22, 'Laco Decorativo',           'UN', 100.000,   0.9000),
    (23, 'Caixa Presente',            'CX',  30.000,   7.8000),
    (24, 'Papel Chumbo',              'CX',  20.000,  14.6000),
    (25, 'Insumo Sem Custo',          'UN',  10.000,   0.0000)
) AS v(ord, nome, unidade, minima, custo);

-- ---------------------------------------------------------------------------
-- 4. Produtos (6 categorias, inativos, safra, soft-delete, sem ficha tecnica)
-- ---------------------------------------------------------------------------
INSERT INTO produtos (nome, descricao, categoria, preco_venda, ativo, margem_desejada,
                      inicio_safra, fim_safra, excluido_em, criado_em, criado_por)
SELECT (ARRAY['Ovo Trufado','Ovo Recheado','Ovo ao Leite','Ovo Meio Amargo','Ovo Branco',
              'Ovo Crocante','Ovo Diet','Ovo Vegano','Ovo Gourmet','Barra Recheada'])[1 + (g % 10)]
       || ' ' || (ARRAY['250g','350g','500g','750g','1kg'])[1 + (g % 5)] || ' #' || g,
       CASE WHEN g % 8 = 0 THEN NULL ELSE 'Produto de teste numero ' || g END,
       (ARRAY['TRUFADO','RECHEADO','DIET','VEGANO','TRADICIONAL','ESPECIAL'])[1 + (g % 6)],
       round((45 + (g % 12) * 8.5)::numeric, 2),
       g % 11 <> 0,
       round((40 + (g % 5) * 7)::numeric, 2),
       CASE WHEN g % 4 = 0 THEN make_date(2026, 2, 1) END,
       CASE WHEN g % 4 = 0 THEN make_date(2026, 4, 30) END,
       CASE WHEN g >= 38 THEN now() - make_interval(days => (30)::int) END,
       now() - make_interval(days => (300 - g * 2)::int), 'admin'
FROM generate_series(1, 40) g;

-- ---------------------------------------------------------------------------
-- 5. Fichas tecnicas: produtos 1..30 tem ficha; 28 e 29 sem itens; 30 com rendimento zero
-- ---------------------------------------------------------------------------
INSERT INTO fichas_tecnicas (produto_id, rendimento, unidade_rendimento, observacoes)
SELECT g,
       CASE WHEN g = 30 THEN 0.000 ELSE 1.000 + (g % 3) END,
       'UN',
       CASE WHEN g % 7 = 0 THEN 'Ficha revisada na safra 2026' END
FROM generate_series(1, 30) g;

INSERT INTO fichas_tecnicas_itens (ficha_tecnica_id, materia_prima_id, quantidade)
SELECT f.id, mp.id, mp.qtd
FROM fichas_tecnicas f
JOIN produtos p ON p.id = f.produto_id
CROSS JOIN LATERAL (
    SELECT 1 + ((f.produto_id * 5 + k * 3) % 25) AS id,
           round((0.050 + ((f.produto_id + k) % 8) * 0.075)::numeric, 3) AS qtd
    FROM generate_series(1, 3 + (f.produto_id % 4)) k
) mp
WHERE f.produto_id NOT IN (28, 29)
ON CONFLICT (ficha_tecnica_id, materia_prima_id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 6. Clientes (segmentos, canais, aniversariantes, LGPD, soft-delete)
-- ---------------------------------------------------------------------------
INSERT INTO clientes (nome, telefone, email, endereco, cpf, excluido_em, preferencia_canal, opt_in,
                      data_cadastro, data_consentimento, anonimizado, segmento, data_nascimento, criado_por)
SELECT CASE WHEN g % 160 = 0 THEN 'Cliente Anonimizado ' || g
            ELSE (ARRAY['Maria','Joao','Ana','Pedro','Luiza','Carlos','Fernanda','Rafael','Juliana','Marcos',
                        'Patricia','Bruno','Camila','Diego','Helena','Tiago','Beatriz','Andre','Larissa','Gustavo'])[1 + (g % 20)]
                 || ' ' || (ARRAY['Silva','Souza','Oliveira','Santos','Pereira','Costa','Almeida','Ribeiro','Gomes','Martins'])[1 + (g % 10)]
                 || ' ' || g END,
       CASE WHEN g % 17 = 0 OR g % 160 = 0 THEN NULL ELSE '(11) 9' || lpad((70000000 + g * 71)::text, 8, '0') END,
       CASE WHEN g % 13 = 0 OR g % 160 = 0 THEN NULL ELSE 'cliente' || g || '@teste.local' END,
       CASE WHEN g % 9 = 0 THEN NULL ELSE 'Rua das Amendoas, ' || (100 + g) || ' - Sao Paulo/SP' END,
       CASE WHEN g % 7 = 0 THEN NULL ELSE lpad((10000000000 + g * 137)::text, 11, '0') END,
       CASE WHEN g % 80 = 0 THEN now() - make_interval(days => (15)::int) END,
       (ARRAY['WHATSAPP','EMAIL','AMBOS','NENHUM'])[1 + (g % 4)],
       g % 3 <> 0,
       now() - make_interval(days => (900 - (g % 850))::int),
       CASE WHEN g % 3 <> 0 THEN now() - make_interval(days => (900 - (g % 850))::int) END,
       g % 160 = 0,
       (ARRAY['NOVO','REGULAR','VIP','INATIVO'])[1 + (g % 4)],
       CASE WHEN g % 53 = 0 THEN make_date(1980 + (g % 20), EXTRACT(MONTH FROM CURRENT_DATE)::int, EXTRACT(DAY FROM CURRENT_DATE)::int)
            WHEN g % 20 = 0 THEN (CURRENT_DATE + (g % 7)) - make_interval(years => (35)::int)
            WHEN g % 5 = 0 THEN NULL
            ELSE make_date(1960 + (g % 45), 1 + (g % 12), 1 + (g % 27)) END,
       'admin'
FROM generate_series(1, :qtd_clientes) g;

-- ---------------------------------------------------------------------------
-- 7. Pedidos: 3 safras, 6 status, atrasados / hoje / proximos dias / historico
-- ---------------------------------------------------------------------------
INSERT INTO pedidos (cliente_id, data_pedido, data_entrega, status, observacoes, total_pedido,
                     token_acompanhamento, slot_entrega)
SELECT CASE WHEN (1 + (b.g % :qtd_clientes)) % 80 = 0 THEN 1 + (b.g % :qtd_clientes) - 1 ELSE 1 + (b.g % :qtd_clientes) END,
       (COALESCE(b.de, CURRENT_DATE) - (3 + (b.g % 18)))::timestamp + make_interval(hours => (8 + (b.g % 9))::int, mins => ((b.g % 4) * 15)::int),
       b.de,
       b.st,
       CASE WHEN b.g % 11 = 0 THEN 'Observacao do pedido de teste ' || b.g END,
       0,
       md5('pedido-' || b.g),
       CASE WHEN b.g % 4 = 0 THEN NULL ELSE make_time(8 + (b.g % 10), (b.g % 4) * 15, 0) END
FROM (
    SELECT g,
           CASE
               WHEN g <= 25  THEN CURRENT_DATE
               WHEN g <= 65  THEN CURRENT_DATE - (1 + (g % 45))
               WHEN g <= 125 THEN CURRENT_DATE + (1 + (g % 7))
               WHEN g % 97 = 0 THEN NULL
               ELSE make_date(2024 + (g % 3), 1 + (g % 12), 1 + (g % 27)) + 5
           END AS de,
           CASE
               WHEN g <= 25  THEN (ARRAY['CONFIRMADO','EM_PRODUCAO','PRONTO'])[1 + (g % 3)]
               WHEN g <= 65  THEN (ARRAY['NOVO','CONFIRMADO','EM_PRODUCAO','PRONTO'])[1 + (g % 4)]
               WHEN g <= 125 THEN (ARRAY['NOVO','CONFIRMADO','EM_PRODUCAO'])[1 + (g % 3)]
               WHEN g % 10 < 7 THEN 'ENTREGUE'
               WHEN g % 10 = 7 THEN 'CANCELADO'
               ELSE (ARRAY['NOVO','CONFIRMADO','EM_PRODUCAO','PRONTO'])[1 + (g % 4)]
           END AS st
    FROM generate_series(1, :qtd_pedidos) g
) b;

-- ---------------------------------------------------------------------------
-- 8. Itens de pedido (custo_unitario nulo em parte dos NOVO)
-- ---------------------------------------------------------------------------
INSERT INTO itens_pedido (pedido_id, produto_id, quantidade, preco_unitario, subtotal, custo_unitario)
SELECT p.id, pr.id, i.qtd, pr.preco_venda, round(pr.preco_venda * i.qtd, 2),
       CASE WHEN p.status <> 'NOVO' OR p.id % 3 = 0 THEN round(pr.preco_venda * 0.38, 2) END
FROM pedidos p
CROSS JOIN LATERAL (
    SELECT 1 + ((p.id * 7 + k * 13) % 37) AS produto_id,
           1 + ((p.id + k) % 6) AS qtd
    FROM generate_series(1, 1 + (p.id % 5)) k
) i
JOIN produtos pr ON pr.id = i.produto_id;

UPDATE pedidos p
SET total_pedido = COALESCE(t.total, 0)
FROM (SELECT pedido_id, SUM(subtotal) AS total FROM itens_pedido GROUP BY pedido_id) t
WHERE t.pedido_id = p.id;

-- ---------------------------------------------------------------------------
-- 9. Pagamentos: quitados, parciais, sem pagamento, cancelados pagos, duplicados sujos
-- ---------------------------------------------------------------------------
INSERT INTO pagamentos (pedido_id, valor, tipo_pagamento, data_pagamento, observacoes)
SELECT p.id, p.total_pedido, (ARRAY['PIX','DINHEIRO','CARTAO_CREDITO','CARTAO_DEBITO','FIADO'])[1 + (p.id % 5)],
       COALESCE(p.data_entrega, p.data_pedido::date), 'Pagamento integral'
FROM pedidos p
WHERE p.status = 'ENTREGUE' AND p.id % 4 <> 0;

INSERT INTO pagamentos (pedido_id, valor, tipo_pagamento, data_pagamento, observacoes)
SELECT p.id, round(p.total_pedido * f.pct, 2),
       (ARRAY['PIX','DINHEIRO','CARTAO_CREDITO','CARTAO_DEBITO','FIADO'])[1 + ((p.id + f.n) % 5)],
       COALESCE(p.data_entrega, p.data_pedido::date) - (f.n * 3), 'Parcela ' || f.n || '/2'
FROM pedidos p
CROSS JOIN (VALUES (1, 0.60), (2, 0.40)) AS f(n, pct)
WHERE p.status = 'ENTREGUE' AND p.id % 4 = 0;

INSERT INTO pagamentos (pedido_id, valor, tipo_pagamento, data_pagamento, observacoes)
SELECT p.id, round(p.total_pedido * (ARRAY[0.20, 0.30, 0.50, 0.70])[1 + (p.id % 4)], 2),
       (ARRAY['PIX','DINHEIRO','CARTAO_CREDITO','CARTAO_DEBITO','FIADO'])[1 + (p.id % 5)],
       p.data_pedido::date + 1, 'Sinal do pedido'
FROM pedidos p
WHERE p.status IN ('CONFIRMADO', 'EM_PRODUCAO', 'PRONTO') AND p.id % 3 <> 0;

INSERT INTO pagamentos (pedido_id, valor, tipo_pagamento, data_pagamento, observacoes)
SELECT p.id, round(p.total_pedido * 0.30, 2), 'PIX', p.data_pedido::date + 1,
       'Sinal recebido antes do cancelamento'
FROM pedidos p
WHERE p.status = 'CANCELADO' AND p.id % 3 = 0;

INSERT INTO pagamentos (pedido_id, valor, tipo_pagamento, data_pagamento, observacoes)
SELECT pg.pedido_id, pg.valor, pg.tipo_pagamento, pg.data_pagamento, 'Lancamento duplicado legado'
FROM pagamentos pg
JOIN pedidos p ON p.id = pg.pedido_id
WHERE p.status = 'ENTREGUE' AND p.id % 401 = 0;

-- ---------------------------------------------------------------------------
-- 10. Ordens de producao (4 status, ordens mistas no mesmo pedido)
-- ---------------------------------------------------------------------------
INSERT INTO ordens_producao (pedido_id, produto_id, quantidade, status, data_abertura, data_conclusao, observacoes)
SELECT ip.pedido_id, ip.produto_id, ip.quantidade,
       CASE
           WHEN p.status = 'CANCELADO' THEN 'CANCELADA'
           WHEN p.status IN ('PRONTO', 'ENTREGUE') THEN 'CONCLUIDA'
           WHEN p.status = 'EM_PRODUCAO' AND ip.id % 3 = 0 THEN 'CONCLUIDA'
           WHEN p.status = 'EM_PRODUCAO' THEN 'EM_ANDAMENTO'
           WHEN p.status = 'CONFIRMADO' AND ip.id % 17 = 0 THEN 'CONCLUIDA'
           ELSE 'PENDENTE'
       END,
       p.data_pedido + make_interval(hours => (6)::int),
       CASE WHEN p.status IN ('PRONTO', 'ENTREGUE')
                 OR (p.status = 'EM_PRODUCAO' AND ip.id % 3 = 0)
                 OR (p.status = 'CONFIRMADO' AND ip.id % 17 = 0)
            THEN p.data_pedido + make_interval(days => (2)::int) END,
       CASE WHEN ip.id % 23 = 0 THEN 'Lote conferido pela confeiteira' END
FROM itens_pedido ip
JOIN pedidos p ON p.id = ip.pedido_id
WHERE p.status <> 'NOVO';

-- ---------------------------------------------------------------------------
-- 11. Movimentacoes de estoque com saldo cumulativo coerente
-- ---------------------------------------------------------------------------
INSERT INTO movimentacoes_estoque (materia_prima_id, tipo, quantidade, saldo_apos, data, motivo, usuario, custo_unitario)
SELECT mp_id, tipo, quantidade,
       SUM(delta) OVER (PARTITION BY mp_id ORDER BY data, seq ROWS UNBOUNDED PRECEDING),
       data, motivo, usuario, custo_unitario
FROM (
    SELECT m.id AS mp_id, 0 AS seq, 'ENTRADA' AS tipo,
           round((500 + m.id * 20)::numeric, 3) AS quantidade,
           round((500 + m.id * 20)::numeric, 3) AS delta,
           make_timestamp(2024, 1, 5, 9, 0, 0) AS data,
           'Estoque inicial da safra 2024' AS motivo, 'admin' AS usuario, m.custo_unitario
    FROM materias_primas m
    UNION ALL
    SELECT m.id, k,
           CASE
               WHEN k % 20 = 0 THEN 'AJUSTE'
               WHEN k % 3 = 0  THEN 'ENTRADA'
               ELSE 'SAIDA'
           END,
           CASE
               WHEN k % 20 = 0 THEN round(((CASE WHEN k % 40 = 0 THEN -1 ELSE 1 END) * (0.100 + (k % 5) * 0.250))::numeric, 3)
               WHEN k % 3 = 0  THEN round((30 + (k % 17) * 4)::numeric, 3)
               ELSE round((0.500 + (k % 9) * 1.250)::numeric, 3)
           END,
           CASE
               WHEN k % 20 = 0 THEN round(((CASE WHEN k % 40 = 0 THEN -1 ELSE 1 END) * (0.100 + (k % 5) * 0.250))::numeric, 3)
               WHEN k % 3 = 0  THEN round((30 + (k % 17) * 4)::numeric, 3)
               ELSE -round((0.500 + (k % 9) * 1.250)::numeric, 3)
           END,
           make_timestamp(2024, 1, 5, 10, 0, 0) + make_interval(hours => (k * 19 + m.id)::int),
           CASE
               WHEN k % 20 = 0 THEN 'Ajuste de inventario | contagem ciclica'
               WHEN k % 3 = 0  THEN 'Compra de reposicao NF ' || (1000 + k)
               ELSE 'Producao | consumo do lote ' || k
           END,
           (ARRAY['admin','confeiteiro','financeiro','Sistema'])[1 + (k % 4)],
           CASE WHEN k % 3 = 0 THEN round(m.custo_unitario * (0.92 + (k % 9) * 0.02), 4) END
    FROM materias_primas m
    CROSS JOIN generate_series(1, :qtd_mov_por_mp) k
) base;

UPDATE materias_primas m
SET quantidade_atual = s.saldo,
    data_ultima_compra = s.ultima_compra
FROM (
    SELECT DISTINCT ON (materia_prima_id)
           materia_prima_id, saldo_apos AS saldo,
           (SELECT MAX(data)::date FROM movimentacoes_estoque x
             WHERE x.materia_prima_id = e.materia_prima_id AND x.tipo = 'ENTRADA') AS ultima_compra
    FROM movimentacoes_estoque e
    ORDER BY materia_prima_id, data DESC, id DESC
) s
WHERE s.materia_prima_id = m.id;

-- 8 materias-primas em nivel critico (quantidade_minima acima do saldo)
UPDATE materias_primas
SET quantidade_minima = round(quantidade_atual * 1.5 + 10, 3)
WHERE id IN (1, 4, 6, 10, 11, 15, 20, 25);

-- ---------------------------------------------------------------------------
-- 12. Qualidade: checklist por produto e inspecoes aprovadas/reprovadas
-- ---------------------------------------------------------------------------
INSERT INTO checklist_qualidade (produto_id, item, ordem, ativo)
SELECT p.id,
       (ARRAY['Temperatura do chocolate','Brilho e acabamento','Peso conferido','Recheio centralizado',
              'Embalagem sem avaria','Rotulo com validade','Ausencia de manchas','Laco bem fixado'])[k],
       k,
       NOT (k = 8 AND p.id % 4 = 0)
FROM produtos p
CROSS JOIN generate_series(1, 5 + (1 + 2)) k
WHERE p.excluido_em IS NULL AND k <= 5 + (p.id % 4);

INSERT INTO inspecao_qualidade (ordem_producao_id, data_inspecao, inspetor, aprovado, observacoes, itens_verificados)
SELECT o.id,
       COALESCE(o.data_conclusao, o.data_abertura) + make_interval(hours => (2)::int),
       (ARRAY['Diego Qualidade','Carla Confeiteira','admin'])[1 + (o.id % 3)],
       o.id % 7 <> 0,
       CASE WHEN o.id % 7 = 0 THEN 'Reprovado: acabamento fora do padrao no lote' END,
       (SELECT jsonb_agg(jsonb_build_object(
                   'checklistItemId', c.id,
                   'descricao', c.item,
                   'verificado', CASE WHEN o.id % 7 = 0 AND c.ordem <= 2 THEN false ELSE true END))
          FROM checklist_qualidade c
         WHERE c.produto_id = o.produto_id AND c.ativo)
FROM ordens_producao o
WHERE o.status = 'CONCLUIDA' AND o.id % 13 = 0;

-- ---------------------------------------------------------------------------
-- 13. Orcamentos (4 status, convertidos e nao convertidos, validade vencida)
-- ---------------------------------------------------------------------------
INSERT INTO orcamentos (cliente_id, data_criacao, validade, status, total, observacoes, token_aprovacao,
                        pedido_id, criado_em, criado_por)
SELECT CASE WHEN (1 + (g % :qtd_clientes)) % 80 = 0 THEN 1 + (g % :qtd_clientes) - 1 ELSE 1 + (g % :qtd_clientes) END,
       (CURRENT_DATE - (g % 600))::timestamp + make_interval(hours => (9 + (g % 8))::int),
       CASE
           WHEN g % 4 = 3 THEN CURRENT_DATE - (5 + (g % 30))
           WHEN g % 4 = 0 THEN CURRENT_DATE + (1 + (g % 10))
           ELSE CURRENT_DATE + (g % 20) - 5
       END,
       (ARRAY['PENDENTE','APROVADO','RECUSADO','EXPIRADO'])[1 + (g % 4)],
       0,
       CASE WHEN g % 6 = 0 THEN NULL ELSE 'Orcamento de teste ' || g END,
       md5('orcamento-' || g),
       CASE WHEN g % 4 = 1 AND g % 8 = 1 THEN 1 + (g % :qtd_pedidos) END,
       now() - make_interval(days => (g % 600)::int), 'admin'
FROM generate_series(1, :qtd_orcamentos) g;

INSERT INTO orcamento_itens (orcamento_id, produto_id, quantidade, preco_unitario, subtotal)
SELECT o.id, pr.id, i.qtd, pr.preco_venda, round(pr.preco_venda * i.qtd, 2)
FROM orcamentos o
CROSS JOIN LATERAL (
    SELECT 1 + ((o.id * 11 + k * 5) % 37) AS produto_id, 1 + ((o.id + k) % 4) AS qtd
    FROM generate_series(1, 1 + (o.id % 4)) k
) i
JOIN produtos pr ON pr.id = i.produto_id;

UPDATE orcamentos o
SET total = COALESCE(t.total, 0)
FROM (SELECT orcamento_id, SUM(subtotal) AS total FROM orcamento_itens GROUP BY orcamento_id) t
WHERE t.orcamento_id = o.id;

-- ---------------------------------------------------------------------------
-- 14. Financeiro
-- ---------------------------------------------------------------------------
INSERT INTO contas_pagar (fornecedor_id, descricao, valor, vencimento, status, categoria)
SELECT CASE WHEN g % 7 = 0 THEN NULL ELSE 1 + (g % 12) END,
       (ARRAY['Compra de chocolate','Aluguel do espaco','Energia eletrica','Servico de entrega',
              'Manutencao da batedeira','Embalagens','Contador','Internet'])[1 + (g % 8)] || ' #' || g,
       round((150 + (g % 30) * 47.5)::numeric, 2),
       CASE
           WHEN g % 3 = 0 THEN CURRENT_DATE - (5 + (g % 60))
           WHEN g % 3 = 1 THEN CURRENT_DATE + (1 + (g % 30))
           ELSE CURRENT_DATE + (g % 6)
       END,
       (ARRAY['ABERTA','PAGA','VENCIDA'])[1 + (g % 3)],
       (ARRAY['MATERIA_PRIMA','DESPESA_FIXA','SERVICO','OUTROS'])[1 + (g % 4)]
FROM generate_series(1, 200) g;

INSERT INTO despesas_fixas (descricao, valor, periodicidade, ativo, data_inicio, data_cancelamento)
SELECT v.descricao, v.valor, v.periodicidade, v.ativo,
       CURRENT_DATE - make_interval(days => (400 + v.ord * 5)::int),
       CASE WHEN NOT v.ativo THEN CURRENT_DATE - make_interval(days => (40)::int) END
FROM (VALUES
    ( 1, 'Aluguel da cozinha',        2500.00, 'MENSAL',     true),
    ( 2, 'Energia eletrica',           680.00, 'MENSAL',     true),
    ( 3, 'Agua',                       180.00, 'MENSAL',     true),
    ( 4, 'Internet',                   120.00, 'MENSAL',     true),
    ( 5, 'Contador',                   450.00, 'MENSAL',     true),
    ( 6, 'Software de gestao',          99.00, 'MENSAL',     true),
    ( 7, 'Seguro do equipamento',      900.00, 'TRIMESTRAL', true),
    ( 8, 'Manutencao preventiva',      750.00, 'TRIMESTRAL', true),
    ( 9, 'Alvara sanitario',           620.00, 'SEMESTRAL',  true),
    (10, 'Certificacao de qualidade', 1400.00, 'SEMESTRAL',  true),
    (11, 'Telefone fixo',               85.00, 'MENSAL',     false),
    (12, 'Aluguel do deposito antigo', 800.00, 'MENSAL',     false),
    (13, 'Marketing recorrente',       350.00, 'MENSAL',     true),
    (14, 'Uniformes',                  240.00, 'SEMESTRAL',  false),
    (15, 'Licenca de embalagem',       310.00, 'TRIMESTRAL', true)
) AS v(ord, descricao, valor, periodicidade, ativo);

INSERT INTO despesas_variaveis (pedido_id, descricao, valor, categoria)
SELECT p.id,
       (ARRAY['Frete expresso','Embalagem especial','Ingrediente extra','Taxa da maquininha'])[1 + (p.id % 4)],
       round((12 + (p.id % 15) * 3.5)::numeric, 2),
       (ARRAY['FRETE','EMBALAGEM','INGREDIENTE','OUTROS'])[1 + (p.id % 4)]
FROM pedidos p
WHERE p.id % 5 = 0;

INSERT INTO gastos_variaveis (descricao, valor, data_lancamento, categoria, referencia_mes, referencia_ano,
                              observacoes, comprovante_url, criado_em, criado_por, desconsiderar_no_custo, pedido_id)
SELECT (ARRAY['Sacola personalizada','Combustivel da entrega','Anuncio patrocinado','Chocolate avulso',
              'Peca da batedeira','Servico de design','Diversos'])[1 + (g % 7)] || ' #' || g,
       round((25 + (g % 40) * 12.75)::numeric, 2),
       d.dia,
       (ARRAY['EMBALAGEM','TRANSPORTE','MARKETING','MATERIA_PRIMA','EQUIPAMENTO','SERVICO','OUTROS'])[1 + (g % 7)],
       EXTRACT(MONTH FROM d.dia)::int,
       EXTRACT(YEAR FROM d.dia)::int,
       CASE WHEN g % 9 = 0 THEN 'Gasto lancado pelo financeiro' END,
       CASE WHEN g % 12 = 0 THEN 'https://exemplo.local/comprovante-' || g || '.pdf' END,
       d.dia::timestamp + make_interval(hours => (11)::int), 'financeiro',
       g % 10 = 0,
       CASE WHEN g % 4 = 0 THEN 1 + (g % :qtd_pedidos) END
FROM generate_series(1, :qtd_gastos) g
CROSS JOIN LATERAL (SELECT (CURRENT_DATE - (g % 900))::date AS dia) d;

INSERT INTO orcamentos_gasto (categoria, valor_orcado, referencia_mes, referencia_ano)
SELECT cat, round((400 + (m * 37) + (a - 2024) * 250)::numeric, 2), m, a
FROM generate_series(2024, 2026) a
CROSS JOIN generate_series(1, 12) m
CROSS JOIN unnest(ARRAY['EMBALAGEM','TRANSPORTE','MARKETING','MATERIA_PRIMA','EQUIPAMENTO','SERVICO','OUTROS']) cat
ON CONFLICT (categoria, referencia_mes, referencia_ano) DO NOTHING;

UPDATE configuracao_financeira
SET meta_faturamento_mensal = 60000.00,
    margem_desejada_padrao = 55.00,
    aliquota_simples = 6.00;

-- ---------------------------------------------------------------------------
-- 15. contas_receber legado, divergente de proposito (nao deve influenciar o aging)
-- ---------------------------------------------------------------------------
INSERT INTO contas_receber (pedido_id, valor_original, valor_pago, vencimento, status)
SELECT p.id, round(p.total_pedido * 3, 2), 0, CURRENT_DATE - 200, 'ABERTA'
FROM pedidos p
WHERE p.status = 'ENTREGUE'
ORDER BY p.id
LIMIT 20;

-- ---------------------------------------------------------------------------
-- 16. CRM: pontos, notas, campanhas
-- ---------------------------------------------------------------------------
INSERT INTO pontos_fidelidade (cliente_id, pedido_id, pontos, tipo, descricao, data_operacao, data_expiracao)
SELECT p.cliente_id, p.id,
       CASE WHEN p.id % 6 = 0 THEN -(10 + (p.id % 40)) ELSE 10 + (p.id % 90) END,
       CASE WHEN p.id % 6 = 0 THEN 'DEBITO' ELSE 'CREDITO' END,
       CASE WHEN p.id % 6 = 0 THEN 'Resgate de desconto' ELSE 'Pontos do pedido #' || p.id END,
       p.data_pedido,
       CASE WHEN p.id % 6 <> 0 THEN (p.data_pedido + make_interval(days => (365)::int)) END
FROM pedidos p
WHERE p.status IN ('ENTREGUE', 'PRONTO') AND p.id % 7 = 0;

INSERT INTO notas_cliente (cliente_id, texto, criado_em, criado_por)
SELECT 1 + (g % :qtd_clientes),
       (ARRAY['Prefere retirar no balcao','Alergica a castanhas','Cliente indicado por amigo',
              'Sempre pede embalagem de presente','Negocia desconto no volume','Atrasa pagamento com frequencia',
              'Compra recorrente na Pascoa','Pediu contato por WhatsApp apenas'])[1 + (g % 8)],
       now() - make_interval(days => (g % 400)::int),
       (ARRAY['admin','atendente','financeiro'])[1 + (g % 3)]
FROM generate_series(1, 300) g;

INSERT INTO campanha_reengajamento (nome, descricao, segmento, canal, template_id, data_envio, status,
                                    total_destinatarios, total_enviados, criado_em, criado_por)
SELECT 'Campanha ' || (ARRAY['Volta Pascoa','Clientes VIP','Reativacao','Dia das Maes','Black Friday',
                             'Aniversariantes','Novos Clientes','Inativos 90 dias','Pre-venda','Ultima Chance',
                             'Indique um Amigo','Clientes Regulares'])[g],
       'Campanha de teste numero ' || g,
       (ARRAY['NOVO','REGULAR','VIP','INATIVO'])[1 + (g % 4)],
       (ARRAY['WHATSAPP','EMAIL'])[1 + (g % 2)],
       NULL,
       CASE WHEN g % 3 <> 0 THEN now() - make_interval(days => (g * 7)::int) END,
       (ARRAY['RASCUNHO','ENVIADA','ENVIADA'])[1 + (g % 3)],
       50 + g * 17,
       CASE WHEN g % 3 = 0 THEN 0 ELSE 40 + g * 15 END,
       now() - make_interval(days => (g * 8)::int), 'admin'
FROM generate_series(1, 12) g;

-- ---------------------------------------------------------------------------
-- 17. Notificacoes e alertas
-- ---------------------------------------------------------------------------
INSERT INTO templates_notificacao (evento_gatilho, canal, assunto, corpo, ativo, variaveis)
SELECT ev, cn,
       CASE WHEN cn = 'EMAIL' THEN 'Pascoa Artesanal — ' || replace(ev, '_', ' ') END,
       'Ola {{cliente}}, evento ' || ev || ' do pedido {{pedidoId}} no valor de {{total}}.',
       NOT (ev = 'ORCAMENTO_RECUSADO' AND cn = 'WHATSAPP'),
       '{{cliente}},{{pedidoId}},{{total}},{{status}}'
FROM unnest(ARRAY['PEDIDO_CONFIRMADO','PRODUCAO_INICIADA','PEDIDO_PRONTO','PEDIDO_ENTREGUE','PAGAMENTO_RECEBIDO',
                  'PEDIDO_CANCELADO','ORCAMENTO_APROVADO','ORCAMENTO_RECUSADO','ANIVERSARIO_CLIENTE']) ev
CROSS JOIN unnest(ARRAY['EMAIL','WHATSAPP']) cn;

INSERT INTO notificacoes_enviadas (pedido_id, template_id, canal, destinatario, data_envio, status,
                                   mensagem_erro, evento, cliente_id, orcamento_id)
SELECT CASE WHEN g % 5 <> 0 THEN 1 + (g % :qtd_pedidos) END,
       1 + (g % 18),
       (ARRAY['EMAIL','WHATSAPP'])[1 + (g % 2)],
       CASE WHEN g % 2 = 0 THEN 'cliente' || (1 + (g % :qtd_clientes)) || '@teste.local'
            ELSE '(11) 9' || lpad((70000000 + g * 71)::text, 8, '0') END,
       now() - make_interval(days => (g % 365)::int, hours => (g % 24)::int),
       CASE WHEN g % 11 = 0 THEN 'FALHA' ELSE 'ENVIADA' END,
       CASE WHEN g % 11 = 0 THEN 'Falha ao enviar: timeout do provedor apos 3 tentativas' END,
       (ARRAY['PEDIDO_CONFIRMADO','PRODUCAO_INICIADA','PEDIDO_PRONTO','PEDIDO_ENTREGUE','PAGAMENTO_RECEBIDO',
              'PEDIDO_CANCELADO','ORCAMENTO_APROVADO','ORCAMENTO_RECUSADO','ANIVERSARIO_CLIENTE'])[1 + (g % 9)],
       1 + (g % :qtd_clientes),
       CASE WHEN g % 5 = 0 THEN 1 + (g % :qtd_orcamentos) END
FROM generate_series(1, :qtd_notificacoes) g;

INSERT INTO alertas_internos (mensagem, link, icone, cor, lido, criado_em)
SELECT CASE
           WHEN g % 4 = 0 THEN 'Estoque critico de ' || (SELECT nome FROM materias_primas WHERE id = 1 + (g % 25))
           WHEN g % 4 = 1 THEN 'Ordem de producao #' || (100 + g) || ' cancelada em andamento'
           WHEN g % 4 = 2 THEN 'Pedido #' || (200 + g) || ' cancelado com R$ ' || (50 + g * 3) || ' ja recebido — verificar devolucao ao cliente.'
           ELSE 'Inspecao de qualidade reprovada na ordem #' || (300 + g)
       END,
       CASE WHEN g % 4 = 0 THEN '/estoque/movimentacoes' WHEN g % 4 = 1 THEN '/producao' ELSE '/pedidos/' || (1 + g) END,
       (ARRAY['bi-box-seam','bi-clipboard2-x','bi-cash-coin','bi-shield-exclamation'])[1 + (g % 4)],
       (ARRAY['warning','danger','warning','info'])[1 + (g % 4)],
       g % 3 = 0,
       now() - make_interval(days => (g % 60)::int, hours => (g % 24)::int)
FROM generate_series(1, 40) g;

-- ---------------------------------------------------------------------------
-- 18. Auditoria
-- ---------------------------------------------------------------------------
INSERT INTO audit_log (usuario, acao, entidade_tipo, entidade_id, detalhes, criado_em)
SELECT (ARRAY['admin','financeiro','atendente','confeiteiro','qualidade'])[1 + (g % 5)],
       (ARRAY['CONFIRMAR_PEDIDO','CANCELAR_PEDIDO','PEDIDO_PRONTO','ENTREGAR_PEDIDO','REGISTRAR_PAGAMENTO',
              'ENTRADA_ESTOQUE','AJUSTE_ESTOQUE','APROVAR_ORCAMENTO'])[1 + (g % 8)],
       (ARRAY['Pedido','Pedido','Pedido','Pedido','Pagamento','MateriaPrima','MateriaPrima','Orcamento'])[1 + (g % 8)],
       1 + (g % :qtd_pedidos),
       'Registro de auditoria de teste ' || g,
       now() - make_interval(days => (g % 365)::int, hours => (g % 24)::int)
FROM generate_series(1, :qtd_auditoria) g;

COMMIT;

ANALYZE;

-- ---------------------------------------------------------------------------
-- 19. Sanidade: cada cenario precisa aparecer com volume
-- ---------------------------------------------------------------------------
\echo '== Pedidos por status'
SELECT status, count(*), round(SUM(total_pedido), 2) AS valor FROM pedidos GROUP BY status ORDER BY status;

\echo '== Ordens de producao por status'
SELECT status, count(*) FROM ordens_producao GROUP BY status ORDER BY status;

\echo '== Orcamentos por status'
SELECT status, count(*) FROM orcamentos GROUP BY status ORDER BY status;

\echo '== Movimentacoes por tipo / contas a pagar por status / pagamentos por tipo'
SELECT tipo, count(*) FROM movimentacoes_estoque GROUP BY tipo ORDER BY tipo;
SELECT status, count(*) FROM contas_pagar GROUP BY status ORDER BY status;
SELECT tipo_pagamento, count(*) FROM pagamentos GROUP BY tipo_pagamento ORDER BY tipo_pagamento;

\echo '== Clientes por segmento / notificacoes por status / usuarios por role'
SELECT segmento, count(*) FROM clientes GROUP BY segmento ORDER BY segmento;
SELECT status, count(*) FROM notificacoes_enviadas GROUP BY status ORDER BY status;
SELECT role, count(*) FROM usuarios GROUP BY role ORDER BY role;

\echo '== Cenarios operacionais do painel do dia'
SELECT
    (SELECT count(*) FROM pedidos WHERE data_entrega = CURRENT_DATE AND status IN ('NOVO','CONFIRMADO','EM_PRODUCAO','PRONTO')) AS entregar_hoje,
    (SELECT count(*) FROM pedidos WHERE data_entrega < CURRENT_DATE AND status IN ('NOVO','CONFIRMADO','EM_PRODUCAO','PRONTO')) AS atrasados,
    (SELECT count(*) FROM ordens_producao WHERE status IN ('PENDENTE','EM_ANDAMENTO')) AS ordens_abertas,
    (SELECT count(*) FROM pedidos WHERE data_entrega IS NULL) AS sem_data_entrega;

\echo '== Financeiro derivado (a receber)'
SELECT count(*) AS pedidos_com_saldo,
       round(SUM(p.total_pedido - COALESCE((SELECT SUM(g.valor) FROM pagamentos g WHERE g.pedido_id = p.id), 0)), 2) AS total_a_receber
FROM pedidos p
WHERE p.status <> 'CANCELADO'
  AND p.total_pedido > COALESCE((SELECT SUM(g.valor) FROM pagamentos g WHERE g.pedido_id = p.id), 0);

\echo '== Cenarios limitrofes plantados'
SELECT
    (SELECT count(*) FROM produtos p WHERE NOT EXISTS (SELECT 1 FROM fichas_tecnicas f WHERE f.produto_id = p.id)) AS produtos_sem_ficha,
    (SELECT count(*) FROM fichas_tecnicas f WHERE NOT EXISTS (SELECT 1 FROM fichas_tecnicas_itens i WHERE i.ficha_tecnica_id = f.id)) AS fichas_sem_itens,
    (SELECT count(*) FROM fichas_tecnicas WHERE rendimento = 0) AS fichas_rendimento_zero,
    (SELECT count(*) FROM materias_primas WHERE quantidade_atual < quantidade_minima) AS mp_criticas,
    (SELECT count(*) FROM produtos WHERE excluido_em IS NOT NULL) AS produtos_excluidos,
    (SELECT count(*) FROM clientes WHERE excluido_em IS NOT NULL) AS clientes_excluidos,
    (SELECT count(*) FROM clientes WHERE anonimizado) AS clientes_anonimizados,
    (SELECT count(*) FROM clientes WHERE EXTRACT(MONTH FROM data_nascimento) = EXTRACT(MONTH FROM CURRENT_DATE)
                                     AND EXTRACT(DAY FROM data_nascimento) = EXTRACT(DAY FROM CURRENT_DATE)) AS aniversariantes_hoje,
    (SELECT count(*) FROM pedidos p WHERE p.status = 'CANCELADO'
       AND EXISTS (SELECT 1 FROM pagamentos g WHERE g.pedido_id = p.id)) AS cancelados_com_pagamento,
    (SELECT count(*) FROM itens_pedido WHERE custo_unitario IS NULL) AS itens_sem_snapshot_custo,
    (SELECT count(*) FROM contas_receber) AS contas_receber_legado;

\echo '== Invariantes (tudo deve ser zero)'
SELECT
    (SELECT count(*) FROM movimentacoes_estoque WHERE saldo_apos < 0) AS saldo_negativo,
    (SELECT count(*) FROM materias_primas WHERE quantidade_atual < 0) AS estoque_negativo,
    (SELECT count(*) FROM pedidos p
      WHERE p.total_pedido <> COALESCE((SELECT SUM(i.subtotal) FROM itens_pedido i WHERE i.pedido_id = p.id), 0)) AS total_divergente,
    (SELECT count(*) FROM orcamentos o
      WHERE o.total <> COALESCE((SELECT SUM(i.subtotal) FROM orcamento_itens i WHERE i.orcamento_id = o.id), 0)) AS total_orcamento_divergente,
    (SELECT count(*) FROM pedidos WHERE status = 'NOVO'
       AND EXISTS (SELECT 1 FROM ordens_producao o WHERE o.pedido_id = pedidos.id)) AS novo_com_ordem;

\echo '== Volume total por tabela'
SELECT relname, n_live_tup AS linhas
FROM pg_stat_user_tables
WHERE n_live_tup > 0
ORDER BY n_live_tup DESC;

-- ---------------------------------------------------------------------------
-- 20. Reproducao do bug de soft-delete (fora da massa padrao, roda sob demanda)
--
-- Vincular historico a cliente ou produto excluido derruba as telas de lista
-- com FetchNotFoundException (JOIN FETCH sobre entidade filtrada por @SQLRestriction).
-- Para reproduzir:
--   UPDATE pedidos SET cliente_id = 80 WHERE id = 1;          -- /pedidos e / passam a dar 500
--   UPDATE itens_pedido SET produto_id = 39 WHERE pedido_id = 1;  -- /producao passa a dar 500
-- Para voltar ao estado bom, reaplique este script.
-- ---------------------------------------------------------------------------
