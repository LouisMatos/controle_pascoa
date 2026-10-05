-- Seed realista: MEI de ovos de Pascoa artesanais, renda extra, 2 pessoas (dona + ajudante).
-- Pico na Pascoa 2026 (05/04), vendas pequenas nos demais meses (Maes, Namorados, Pais, Criancas).
--
-- ATENCAO: apaga TODOS os dados de negocio (preserva admin, configuracoes e templates_notificacao).
--   docker compose exec -T postgres psql -U postgres -d pascoa_monolith < infra/seed/seed-mei-realista.sql
--
-- Deterministico (hash md5, sem random()). Entregas futuras/recentes seguem CURRENT_DATE.

\set ON_ERROR_STOP on

BEGIN;

CREATE FUNCTION pg_temp.h(n bigint, salt text) RETURNS int LANGUAGE sql IMMUTABLE AS
$$ SELECT abs(('x' || substr(md5(n::text || '-' || salt), 1, 7))::bit(28)::int) $$;

TRUNCATE TABLE
    alertas_internos, audit_log, campanha_reengajamento, checklist_qualidade,
    contas_pagar, contas_receber, despesas_fixas, despesas_variaveis,
    fichas_tecnicas_itens, fichas_tecnicas, gastos_variaveis, inspecao_qualidade,
    itens_pedido, movimentacoes_estoque, notas_cliente, notificacoes_enviadas,
    orcamento_itens, orcamentos, orcamentos_gasto, ordens_producao, pagamentos,
    password_reset_token, pedidos, pontos_fidelidade, produtos, materias_primas,
    fornecedores, clientes
RESTART IDENTITY CASCADE;

DELETE FROM usuarios WHERE login <> 'admin';
UPDATE usuarios SET nome = 'Dona do Negocio', email = NULL WHERE login = 'admin';
INSERT INTO usuarios (nome, login, senha, role, ativo, totp_ativado, tentativas_totp_falhas)
SELECT 'Ajudante da Cozinha', 'ajudante', senha, 'CONFEITEIRO', true, false, 0
FROM usuarios WHERE login = 'admin';

UPDATE configuracao_financeira SET margem_desejada_padrao = 40.00, aliquota_simples = 0.00,
                                   meta_faturamento_mensal = 800.00;

-- Fornecedores ---------------------------------------------------------------
INSERT INTO fornecedores (nome, cnpj, telefone, email, observacoes, criado_em, criado_por)
SELECT v.nome, v.cnpj, v.tel, v.email, v.obs, TIMESTAMP '2025-12-15 10:00', 'admin'
FROM (VALUES
    ('Casa do Confeiteiro',      NULL::text, '(11) 3222-4410', NULL::text,                 'Chocolate em barra e embalagens. Retirada na loja.'),
    ('Atacadao Supermercado',    NULL,       NULL,             NULL,                       'Leite condensado, creme de leite, doce de leite.'),
    ('Embalagens Rapida',        NULL,       '(11) 98877-1020','vendas@embalagensrapida.com.br', 'Caixas, fitas e etiquetas. Entrega em 3 dias.'),
    ('Distribuidora Doce Vida',  NULL,       '(11) 97766-3344', NULL,                      'Pastas, polpas e recheios. Pedido minimo R$ 150.')
) AS v(nome, cnpj, tel, email, obs);

-- Materias-primas ------------------------------------------------------------
CREATE TEMP TABLE _mp (id int, nome text, unidade text, minima numeric, custo numeric, passo numeric, forn int);
INSERT INTO _mp VALUES
    ( 1, 'Chocolate ao Leite (barra)',     'KG', 3.0, 52.00, 0.5, 1),
    ( 2, 'Chocolate Meio Amargo (barra)',  'KG', 3.0, 56.00, 0.5, 1),
    ( 3, 'Chocolate Branco (barra)',       'KG', 2.0, 58.00, 0.5, 1),
    ( 4, 'Leite Condensado 395g',          'UN', 6.0,  6.90, 1,   2),
    ( 5, 'Creme de Leite 200g',            'UN', 6.0,  3.90, 1,   2),
    ( 6, 'Doce de Leite',                  'KG', 1.0, 24.00, 1,   2),
    ( 7, 'Creme de Avela',                 'KG', 0.5, 69.00, 0.5, 4),
    ( 8, 'Polpa de Maracuja',              'KG', 1.0, 18.00, 1,   4),
    ( 9, 'Caixa para Ovo',                 'UN', 20,   3.20, 10,  3),
    (10, 'Fita de Cetim (por ovo)',        'UN', 30,   0.60, 10,  3),
    (11, 'Tag Personalizada',              'UN', 30,   0.45, 10,  3),
    (12, 'Saco de Celofane',               'UN', 30,   0.35, 10,  3),
    (13, 'Caixa para Bombons',             'UN', 10,   4.50, 10,  3),
    (14, 'Forminha de Papel',              'UN', 50,   0.12, 50,  3);

INSERT INTO materias_primas (id, nome, unidade, quantidade_atual, quantidade_minima, custo_unitario,
                             custo_medio_ponderado, fornecedor_preferencial_id, criado_em, criado_por)
OVERRIDING SYSTEM VALUE
SELECT id, nome, unidade, 0, minima, custo, custo, forn, TIMESTAMP '2025-12-15 10:00', 'admin' FROM _mp;
SELECT setval(pg_get_serial_sequence('materias_primas', 'id'), 14);

-- Produtos e fichas tecnicas -------------------------------------------------
INSERT INTO produtos (id, nome, descricao, categoria, preco_venda, ativo, margem_desejada, criado_em, criado_por)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 'Ovo ao Leite 250g',                'Casca de chocolate ao leite com recheio de brigadeiro.',  'TRADICIONAL', 54.90, true, 40, TIMESTAMP '2025-12-15 10:00', 'admin'),
    (2, 'Ovo Meio Amargo 350g',             'Casca meio amargo com ganache.',                          'TRADICIONAL', 74.90, true, 40, TIMESTAMP '2025-12-15 10:00', 'admin'),
    (3, 'Ovo de Brigadeiro 350g',           'Casca ao leite recheada com brigadeiro cremoso.',         'RECHEADO',    79.90, true, 40, TIMESTAMP '2025-12-15 10:00', 'admin'),
    (4, 'Ovo Trufado de Maracuja 350g',     'Casca de chocolate branco, trufa de maracuja.',           'TRUFADO',     84.90, true, 40, TIMESTAMP '2025-12-15 10:00', 'admin'),
    (5, 'Ovo de Doce de Leite 500g',        'Casca ao leite recheada com doce de leite e brigadeiro.', 'RECHEADO',   109.90, true, 40, TIMESTAMP '2025-12-15 10:00', 'admin'),
    (6, 'Ovo Branco com Avela 350g',        'Casca de chocolate branco, recheio de creme de avela.',   'ESPECIAL',    89.90, true, 40, TIMESTAMP '2025-12-15 10:00', 'admin'),
    (7, 'Caixa de Mini Ovos (6 un)',        'Seis mini ovos sortidos ao leite e meio amargo.',         'TRADICIONAL', 34.90, true, 40, TIMESTAMP '2025-12-15 10:00', 'admin'),
    (8, 'Caixa de Trufas (12 un)',          'Doze trufas de chocolate meio amargo.',                   'TRUFADO',     44.90, true, 40, TIMESTAMP '2025-12-15 10:00', 'admin');
SELECT setval(pg_get_serial_sequence('produtos', 'id'), 8);

INSERT INTO fichas_tecnicas (produto_id, rendimento, unidade_rendimento, observacoes)
SELECT id, 1, 'UN', NULL FROM produtos;

CREATE TEMP TABLE _ficha (produto_id int, mp int, qtd numeric);
INSERT INTO _ficha VALUES
    (1, 1, 0.150), (1, 4, 0.3), (1, 5, 0.3), (1, 9, 1), (1, 10, 1), (1, 11, 1), (1, 12, 1),
    (2, 2, 0.300), (2, 5, 0.4), (2, 9, 1), (2, 10, 1), (2, 11, 1), (2, 12, 1),
    (3, 1, 0.200), (3, 4, 1), (3, 5, 0.5), (3, 9, 1), (3, 10, 1), (3, 11, 1), (3, 12, 1),
    (4, 3, 0.200), (4, 8, 0.150), (4, 5, 0.5), (4, 9, 1), (4, 10, 1), (4, 11, 1), (4, 12, 1),
    (5, 1, 0.300), (5, 6, 0.250), (5, 4, 1), (5, 9, 1), (5, 10, 1), (5, 11, 1), (5, 12, 1),
    (6, 3, 0.200), (6, 7, 0.150), (6, 5, 0.5), (6, 9, 1), (6, 10, 1), (6, 11, 1), (6, 12, 1),
    (7, 1, 0.120), (7, 2, 0.080), (7, 13, 1), (7, 11, 1),
    (8, 2, 0.150), (8, 5, 0.75), (8, 14, 12), (8, 13, 1);

INSERT INTO fichas_tecnicas_itens (ficha_tecnica_id, materia_prima_id, quantidade)
SELECT ft.id, f.mp, f.qtd FROM _ficha f JOIN fichas_tecnicas ft ON ft.produto_id = f.produto_id;

CREATE TEMP TABLE _custo AS
SELECT f.produto_id, round(sum(f.qtd * m.custo), 2) AS custo
FROM _ficha f JOIN _mp m ON m.id = f.mp GROUP BY f.produto_id;

INSERT INTO checklist_qualidade (produto_id, item, ordem, ativo)
SELECT p.id, c.item, c.ordem, true
FROM produtos p CROSS JOIN (VALUES
    ('Temperatura do chocolate na temperagem', 1),
    ('Casca sem trincas ou bolhas', 2),
    ('Brilho e acabamento uniformes', 3),
    ('Recheio na quantidade da ficha', 4),
    ('Embalagem, fita e tag conferidas', 5)) AS c(item, ordem);

-- Clientes -------------------------------------------------------------------
CREATE TEMP TABLE _cli (id int, nome text, canal text, email boolean);
INSERT INTO _cli VALUES
    ( 1, 'Mariana Souza',         'WHATSAPP', true ), ( 2, 'Camila Ferreira',      'WHATSAPP', false),
    ( 3, 'Patricia Lima',         'AMBOS',    true ), ( 4, 'Renata Carvalho',      'WHATSAPP', false),
    ( 5, 'Juliana Mendes',        'WHATSAPP', true ), ( 6, 'Fernanda Rocha',       'WHATSAPP', false),
    ( 7, 'Aline Barbosa',         'WHATSAPP', false), ( 8, 'Carolina Duarte',      'AMBOS',    true ),
    ( 9, 'Beatriz Nogueira',      'WHATSAPP', false), (10, 'Luciana Teixeira',     'WHATSAPP', false),
    (11, 'Tatiane Moreira',       'WHATSAPP', false), (12, 'Vanessa Araujo',       'EMAIL',    true ),
    (13, 'Priscila Gomes',        'WHATSAPP', false), (14, 'Simone Cardoso',       'WHATSAPP', false),
    (15, 'Debora Freitas',        'WHATSAPP', true ), (16, 'Eliane Ramos',         'WHATSAPP', false),
    (17, 'Claudia Pires',         'WHATSAPP', false), (18, 'Sandra Batista',       'NENHUM',   false),
    (19, 'Rosangela Monteiro',    'WHATSAPP', false), (20, 'Marta Correia',        'WHATSAPP', false),
    (21, 'Rodrigo Alves',         'WHATSAPP', false), (22, 'Felipe Martins',       'WHATSAPP', true ),
    (23, 'Bruno Cavalcanti',      'WHATSAPP', false), (24, 'Thiago Ribeiro',       'WHATSAPP', false),
    (25, 'Andre Nunes',           'EMAIL',    true ), (26, 'Leandro Farias',       'WHATSAPP', false),
    (27, 'Gustavo Pinto',         'WHATSAPP', false), (28, 'Ricardo Barros',       'WHATSAPP', false),
    (29, 'Tia Neide',             'WHATSAPP', false), (30, 'Dona Lourdes (vizinha)','NENHUM',  false),
    (31, 'Paulo Henrique Dias',   'WHATSAPP', false), (32, 'Marcelo Azevedo',      'WHATSAPP', false);

INSERT INTO clientes (id, nome, telefone, email, endereco, preferencia_canal, opt_in, data_cadastro,
                      data_consentimento, anonimizado, segmento, data_nascimento, criado_por)
OVERRIDING SYSTEM VALUE
SELECT id, nome,
       CASE WHEN canal = 'EMAIL' AND id % 2 = 0 THEN NULL ELSE '(11) 9' || lpad((70000000 + id * 91733 % 29999999)::text, 8, '0') END,
       CASE WHEN email THEN lower(split_part(nome, ' ', 1)) || '.' || id || '@email.com' END,
       NULL,
       canal, canal <> 'NENHUM',
       TIMESTAMP '2026-01-10 12:00' + make_interval(days => id * 3),
       CASE WHEN canal <> 'NENHUM' THEN TIMESTAMP '2026-01-10 12:00' + make_interval(days => id * 3) END,
       false, 'NOVO',
       CASE WHEN id % 3 = 0 THEN DATE '1980-01-01' + (id * 811) % 6000 END,
       'admin'
FROM _cli;
SELECT setval(pg_get_serial_sequence('clientes', 'id'), 32);

-- Pedidos --------------------------------------------------------------------
CREATE TEMP TABLE _per (ini date, fim date, qtd int, pico boolean);
INSERT INTO _per VALUES
    (DATE '2026-01-10', DATE '2026-01-30',  5, false),
    (DATE '2026-02-05', DATE '2026-02-27',  8, false),
    (DATE '2026-03-14', DATE '2026-03-31', 30, true),
    (DATE '2026-04-01', DATE '2026-04-04', 26, true),
    (DATE '2026-04-10', DATE '2026-04-30',  3, false),
    (DATE '2026-05-01', DATE '2026-05-10', 10, false),
    (DATE '2026-06-01', DATE '2026-06-12',  7, false),
    (DATE '2026-07-01', DATE '2026-07-31',  5, false),
    (DATE '2026-08-01', DATE '2026-08-10',  6, false),
    (DATE '2026-09-01', DATE '2026-09-30',  8, false),
    (CURRENT_DATE - 3,  CURRENT_DATE + 10, 15, false);

CREATE TEMP TABLE _ped AS
WITH base AS (
    SELECT row_number() OVER (ORDER BY p.ini, g) AS n, p.pico,
           p.ini + (pg_temp.h(p.ini::date - DATE '2026-01-01' + g, 'e') % (p.fim - p.ini + 1)) AS entrega
    FROM _per p CROSS JOIN LATERAL generate_series(1, p.qtd) g
)
SELECT n, pico, entrega,
       1 + sqrt((pg_temp.h(n, 'a') % 100) * (pg_temp.h(n, 'b') % 100)) / 100.0 * 31 AS cli_f,
       GREATEST(LEAST(entrega - (CASE WHEN pico THEN 5 + pg_temp.h(n, 'l') % 14 ELSE 3 + pg_temp.h(n, 'l') % 10 END),
                      CURRENT_DATE - 1 - pg_temp.h(n, 'l2') % 4),
                CASE WHEN entrega >= date_trunc('month', CURRENT_DATE) THEN date_trunc('month', CURRENT_DATE)::date
                     ELSE DATE '2000-01-01' END) AS dia_pedido,
       (entrega BETWEEN DATE '2026-03-10' AND DATE '2026-04-04') AS safra
FROM base;

ALTER TABLE _ped ADD COLUMN cliente_id int, ADD COLUMN status text, ADD COLUMN pedido_id bigint;
UPDATE _ped SET cliente_id = LEAST(31, floor(cli_f)::int),
    status = CASE
        WHEN pg_temp.h(n, 'x') % 40 = 0 THEN 'CANCELADO'
        WHEN entrega < CURRENT_DATE THEN 'ENTREGUE'
        WHEN entrega = CURRENT_DATE THEN 'PRONTO'
        WHEN entrega <= CURRENT_DATE + 3 THEN 'EM_PRODUCAO'
        WHEN pg_temp.h(n, 's') % 4 = 0 THEN 'NOVO'
        ELSE 'CONFIRMADO' END;

INSERT INTO pedidos (cliente_id, data_pedido, data_entrega, status, observacoes, total_pedido,
                     token_acompanhamento, slot_entrega)
SELECT cliente_id,
       dia_pedido::timestamp + make_interval(hours => 8 + pg_temp.h(n, 'hr') % 13, mins => (pg_temp.h(n, 'mn') % 4) * 15),
       entrega, status,
       CASE pg_temp.h(n, 'ob') % 12
           WHEN 0 THEN 'Presente: escrever "Feliz Pascoa" na tag.'
           WHEN 1 THEN 'Sem castanhas, filho alergico.'
           WHEN 2 THEN 'Retirada na minha casa.'
           WHEN 3 THEN 'Entregar no trabalho depois das 14h.' END,
       0, md5('mei-' || n),
       make_time(9 + pg_temp.h(n, 'sl') % 9, 0, 0)
FROM _ped ORDER BY n;

UPDATE _ped p SET pedido_id = pe.id FROM pedidos pe WHERE pe.token_acompanhamento = md5('mei-' || p.n);

-- Itens ----------------------------------------------------------------------
CREATE TEMP TABLE _it AS
WITH linhas AS (
    SELECT p.n, p.safra, k
    FROM _ped p
    CROSS JOIN LATERAL generate_series(1,
        1 + (pg_temp.h(p.n, 'k1') % 3 = 0)::int + (p.pico AND pg_temp.h(p.n, 'k2') % 6 = 0)::int) k
), sorteio AS (
    SELECT n, k, safra, pg_temp.h(n * 10 + k, 'pr') % 100 AS r FROM linhas
), prod AS (
    SELECT n, k, CASE
        WHEN safra THEN CASE WHEN r < 20 THEN 1 WHEN r < 35 THEN 2 WHEN r < 55 THEN 3 WHEN r < 70 THEN 4
                             WHEN r < 78 THEN 5 WHEN r < 85 THEN 6 WHEN r < 93 THEN 7 ELSE 8 END
        ELSE CASE WHEN r < 12 THEN 1 WHEN r < 17 THEN 3 WHEN r < 62 THEN 7 ELSE 8 END END AS produto_id
    FROM sorteio
)
SELECT n, produto_id,
       sum(CASE WHEN produto_id <= 6 THEN 1 + (pg_temp.h(n * 10 + k, 'q') % 5 = 0)::int
                ELSE 1 + (pg_temp.h(n * 10 + k, 'q') % 3 = 0)::int END) AS quantidade
FROM prod GROUP BY n, produto_id;

INSERT INTO itens_pedido (pedido_id, produto_id, quantidade, preco_unitario, subtotal, custo_unitario)
SELECT p.pedido_id, i.produto_id, i.quantidade, pr.preco_venda, i.quantidade * pr.preco_venda, c.custo
FROM _it i JOIN _ped p ON p.n = i.n
JOIN produtos pr ON pr.id = i.produto_id JOIN _custo c ON c.produto_id = i.produto_id
ORDER BY i.n, i.produto_id;

UPDATE pedidos pe SET total_pedido = t.total
FROM (SELECT pedido_id, sum(quantidade * preco_unitario) AS total FROM itens_pedido GROUP BY pedido_id) t
WHERE t.pedido_id = pe.id;

UPDATE pedidos pe SET custo_real_calculado = t.custo,
                      margem_real_calculada = round((pe.total_pedido - t.custo) / pe.total_pedido * 100, 2)
FROM (SELECT pedido_id, sum(quantidade * custo_unitario) AS custo FROM itens_pedido GROUP BY pedido_id) t
WHERE t.pedido_id = pe.id AND pe.status = 'ENTREGUE';

-- Producao -------------------------------------------------------------------
INSERT INTO ordens_producao (pedido_id, produto_id, quantidade, status, data_abertura, data_conclusao)
SELECT p.pedido_id, i.produto_id, i.quantidade,
       CASE WHEN p.status = 'CANCELADO' THEN 'CANCELADA'
            WHEN p.status IN ('ENTREGUE', 'PRONTO') THEN 'CONCLUIDA'
            WHEN p.status = 'EM_PRODUCAO' THEN CASE WHEN pg_temp.h(i.n, 'op') % 2 = 0 THEN 'EM_ANDAMENTO' ELSE 'PENDENTE' END
            ELSE 'PENDENTE' END,
       pe.data_pedido + interval '1 day',
       CASE WHEN p.status IN ('ENTREGUE', 'PRONTO')
            THEN GREATEST(pe.data_pedido + interval '1 day 2 hours', p.entrega::timestamp - interval '1 day' + interval '16 hours') END
FROM _it i JOIN _ped p ON p.n = i.n JOIN pedidos pe ON pe.id = p.pedido_id
WHERE p.status <> 'NOVO';

-- Financeiro: pagamentos e contas a receber ----------------------------------
CREATE TEMP TABLE _pg (pedido_id bigint, valor numeric, tipo text, data date);
INSERT INTO _pg
SELECT p.pedido_id, round(pe.total_pedido / 2, 2), 'PIX', pe.data_pedido::date + 1
FROM _ped p JOIN pedidos pe ON pe.id = p.pedido_id
WHERE (p.status = 'ENTREGUE' AND pe.total_pedido >= 100 AND pg_temp.h(p.n, 'sn') % 10 < 4)
   OR (p.status IN ('CONFIRMADO', 'EM_PRODUCAO', 'PRONTO') AND pg_temp.h(p.n, 'sn') % 2 = 0);

INSERT INTO _pg
SELECT p.pedido_id, pe.total_pedido - COALESCE(s.valor, 0),
       CASE WHEN pg_temp.h(p.n, 'tp') % 100 < 55 THEN 'PIX' WHEN pg_temp.h(p.n, 'tp') % 100 < 70 THEN 'DINHEIRO'
            WHEN pg_temp.h(p.n, 'tp') % 100 < 85 THEN 'CARTAO_CREDITO' ELSE 'CARTAO_DEBITO' END,
       p.entrega
FROM _ped p JOIN pedidos pe ON pe.id = p.pedido_id LEFT JOIN _pg s ON s.pedido_id = p.pedido_id
WHERE p.status = 'ENTREGUE' AND pg_temp.h(p.n, 'fi') % 30 <> 0;

INSERT INTO pagamentos (pedido_id, valor, tipo_pagamento, data_pagamento)
SELECT pedido_id, valor, tipo, data FROM _pg;

INSERT INTO contas_receber (pedido_id, valor_original, valor_pago, vencimento, status)
SELECT p.pedido_id, pe.total_pedido, COALESCE(g.pago, 0),
       CASE WHEN COALESCE(g.pago, 0) = 0 AND p.status = 'ENTREGUE' THEN p.entrega + 15 ELSE p.entrega END,
       CASE WHEN COALESCE(g.pago, 0) >= pe.total_pedido THEN 'PAGA'
            WHEN (CASE WHEN COALESCE(g.pago, 0) = 0 AND p.status = 'ENTREGUE' THEN p.entrega + 15 ELSE p.entrega END) < CURRENT_DATE THEN 'VENCIDA'
            ELSE 'ABERTA' END
FROM _ped p JOIN pedidos pe ON pe.id = p.pedido_id
LEFT JOIN (SELECT pedido_id, sum(valor) AS pago FROM _pg GROUP BY pedido_id) g ON g.pedido_id = p.pedido_id
WHERE p.status NOT IN ('NOVO', 'CANCELADO');

-- Estoque: compra no fim do mes anterior, consumo agregado no fim do mes -----
CREATE TEMP TABLE _cons AS
SELECT f.mp, date_trunc('month', o.data_abertura)::date AS mes, sum(o.quantidade * f.qtd) AS qtd
FROM ordens_producao o JOIN _ficha f ON f.produto_id = o.produto_id
WHERE o.status IN ('CONCLUIDA', 'EM_ANDAMENTO')
GROUP BY f.mp, date_trunc('month', o.data_abertura);

CREATE TEMP TABLE _mov AS
SELECT c.mp, 'ENTRADA'::text AS tipo, ceil(c.qtd * 1.2 / m.passo) * m.passo AS qtd,
       (c.mes - 2)::timestamp + interval '10 hours' AS data,
       'Compra do mes - ' || to_char(c.mes, 'MM/YYYY') AS motivo, m.custo
FROM _cons c JOIN _mp m ON m.id = c.mp
UNION ALL
SELECT c.mp, 'SAIDA', round(c.qtd, 3),
       LEAST((c.mes + interval '1 month' - interval '1 day')::timestamp + interval '18 hours', now() - interval '1 hour'),
       'Consumo da producao - ' || to_char(c.mes, 'MM/YYYY'), m.custo
FROM _cons c JOIN _mp m ON m.id = c.mp;

INSERT INTO movimentacoes_estoque (materia_prima_id, tipo, quantidade, saldo_apos, data, motivo, usuario, custo_unitario)
SELECT mp, tipo, qtd,
       sum(CASE WHEN tipo = 'ENTRADA' THEN qtd ELSE -qtd END) OVER (PARTITION BY mp ORDER BY data, tipo),
       data, motivo, 'admin', custo
FROM _mov ORDER BY data, mp;

UPDATE materias_primas m SET
    quantidade_atual = COALESCE((SELECT sum(CASE WHEN tipo = 'ENTRADA' THEN quantidade ELSE -quantidade END)
                                 FROM movimentacoes_estoque WHERE materia_prima_id = m.id), 0),
    data_ultima_compra = (SELECT max(data)::date FROM movimentacoes_estoque WHERE materia_prima_id = m.id AND tipo = 'ENTRADA');

-- Despesas -------------------------------------------------------------------
INSERT INTO despesas_fixas (descricao, valor, periodicidade, ativo, data_inicio) VALUES
    ('DAS-MEI (guia mensal)',                     81.05, 'MENSAL', true, DATE '2026-01-01'),
    ('Gas e energia da cozinha (rateio da casa)', 120.00, 'MENSAL', true, DATE '2026-01-01');

INSERT INTO contas_pagar (fornecedor_id, descricao, valor, vencimento, status, categoria)
SELECT NULL, 'DAS-MEI ' || to_char(m, 'MM/YYYY'), 81.05, m + 19,
       CASE WHEN m + 19 < CURRENT_DATE THEN 'PAGA' ELSE 'ABERTA' END, 'DESPESA_FIXA'
FROM (SELECT d::date AS m FROM generate_series(DATE '2026-01-01', date_trunc('month', CURRENT_DATE)::date, interval '1 month') d) x;

INSERT INTO contas_pagar (fornecedor_id, descricao, valor, vencimento, status, categoria)
VALUES (4, 'Pasta de avela e polpas - pedido proximo mes', 320.00, CURRENT_DATE + 10, 'ABERTA', 'MATERIA_PRIMA');

INSERT INTO gastos_variaveis (descricao, valor, data_lancamento, categoria, referencia_mes, referencia_ano, observacoes, criado_em, criado_por)
SELECT v.descricao, v.valor, v.data, v.cat, EXTRACT(MONTH FROM v.data)::int, EXTRACT(YEAR FROM v.data)::int, v.obs, v.data::timestamp + interval '9 hours', 'admin'
FROM (VALUES
    ('Termometro culinario digital',        45.00::numeric, DATE '2026-01-14', 'EQUIPAMENTO', NULL::text),
    ('Formas de acetato para ovos',        120.00,          DATE '2026-02-10', 'EQUIPAMENTO', '3 tamanhos'),
    ('Impulsionamento no Instagram',        40.00,          DATE '2026-02-20', 'MARKETING',   NULL),
    ('Impulsionamento no Instagram',        60.00,          DATE '2026-03-12', 'MARKETING',   'Campanha de Pascoa'),
    ('Etiquetas e tags personalizadas',     89.00,          DATE '2026-03-05', 'EMBALAGEM',   NULL),
    ('Gas de cozinha (botijao extra)',     130.00,          DATE '2026-03-18', 'OUTROS',      'Producao intensa'),
    ('Combustivel das entregas',            85.00,          DATE '2026-04-03', 'TRANSPORTE',  'Entregas da semana de Pascoa'),
    ('Impulsionamento no Instagram',        30.00,          DATE '2026-05-04', 'MARKETING',   'Dia das Maes'),
    ('Gas de cozinha',                     130.00,          DATE '2026-07-02', 'OUTROS',      NULL),
    ('Impulsionamento no Instagram',        30.00,          DATE '2026-10-02', 'MARKETING',   'Dia das Criancas')
) AS v(descricao, valor, data, cat, obs);

-- Segmento do CRM (mesmas regras do CrmService) ------------------------------
UPDATE clientes c SET segmento = CASE
    WHEN s.pedidos = 0 THEN 'NOVO'
    WHEN s.ultimo < now() - interval '90 days' THEN 'INATIVO'
    WHEN s.ltv >= 300 OR s.pedidos >= 3 THEN 'VIP'
    ELSE 'REGULAR' END
FROM (SELECT c2.id, count(p.id) FILTER (WHERE p.status <> 'CANCELADO') AS pedidos,
             COALESCE(sum(p.total_pedido) FILTER (WHERE p.status <> 'CANCELADO'), 0) AS ltv,
             max(p.data_pedido) AS ultimo
      FROM clientes c2 LEFT JOIN pedidos p ON p.cliente_id = c2.id GROUP BY c2.id) s
WHERE s.id = c.id;

COMMIT;

SELECT 'pedidos' AS item, count(*) FROM pedidos UNION ALL
SELECT 'itens', count(*) FROM itens_pedido UNION ALL
SELECT 'ordens', count(*) FROM ordens_producao UNION ALL
SELECT 'clientes', count(*) FROM clientes UNION ALL
SELECT 'faturamento', round(sum(total_pedido)) FROM pedidos WHERE status <> 'CANCELADO';
