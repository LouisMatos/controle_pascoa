-- Massa de CENARIOS do pascoa-monolith: >=10 casos nomeados por fluxo/jornada.
--
-- Aditivo e idempotente: nao apaga nada, reexecutar nao duplica. Roda sobre banco vazio
-- (pos-Flyway + admin) ou sobre infra/seed/seed-massa-teste.sql.
--   docker compose exec -T postgres psql -U postgres -d pascoa_monolith < infra/seed/seed-cenarios.sql
--
-- Marcadores: nome/descricao com [CEN-<FLUXO>-NN]; usuarios com login cen_*; tokens publicos fixos:
--   pedido    /acompanhamento/00000ce0-0001-4000-8000-0000000000NN   (NN = 01..25, 101..155, 201..212, 301..352, 401..460)
--   orcamento /orcamento-publico/00000ce0-0002-4000-8000-0000000000NN (NN = 01..14, 101..155)
--   reset     /auth/reset-password?token=00000ce0-0003-4000-8000-0000000000NN (NN = 01..10)
-- Senha dos usuarios cen_* = a do admin. 2FA (TOTP) dos cen_admin2fa / cen_admin_bloqueado: JBSWY3DPEHPK3PXP.
-- Datas relativas a CURRENT_DATE: os cenarios (atrasado, hoje, +2 dias, mes corrente) nao envelhecem.
-- Se o banco veio de seed-massa-teste.sql, orcamentos_gasto do mes corrente ja existe e prevalece (ON CONFLICT DO NOTHING).

\set ON_ERROR_STOP on

BEGIN;

-- ---------------------------------------------------------------------------
-- 1. Usuarios (CEN-USR): 6 roles, inativo, sem email, 2FA ativo/pendente/bloqueado
-- ---------------------------------------------------------------------------
INSERT INTO usuarios (nome, login, senha, role, ativo, totp_secret, totp_ativado, tentativas_totp_falhas, email)
SELECT v.nome || ' [CEN-USR-' || lpad(v.n::text, 2, '0') || ']', v.login, (SELECT senha FROM usuarios WHERE login = 'admin'),
       v.role, v.ativo, v.secret, v.ativado, v.falhas, v.email
FROM (VALUES
    ( 1, 'Admin Cenario',          'cen_admin',           'ADMIN',            true,  NULL,               false, 0, 'cen_admin@pascoa.local'),
    ( 2, 'Financeiro Cenario',     'cen_financeiro',      'FINANCEIRO',       true,  NULL,               false, 0, 'cen_financeiro@pascoa.local'),
    ( 3, 'Atendente Cenario',      'cen_atendente',       'ATENDENTE',        true,  NULL,               false, 0, 'cen_atendente@pascoa.local'),
    ( 4, 'Confeiteiro Cenario',    'cen_confeiteiro',     'CONFEITEIRO',      true,  NULL,               false, 0, 'cen_confeiteiro@pascoa.local'),
    ( 5, 'Qualidade Cenario',      'cen_qualidade',       'GESTOR_QUALIDADE', true,  NULL,               false, 0, 'cen_qualidade@pascoa.local'),
    ( 6, 'Analista Cenario',       'cen_analista',        'ANALISTA',         true,  NULL,               false, 0, 'cen_analista@pascoa.local'),
    ( 7, 'Admin 2FA Ativo',        'cen_admin2fa',        'ADMIN',            true,  'JBSWY3DPEHPK3PXP', true,  0, 'cen_admin2fa@pascoa.local'),
    ( 8, 'Admin 2FA Pendente',     'cen_admin2fa_setup',  'ADMIN',            true,  NULL,               false, 0, 'cen_admin2fa_setup@pascoa.local'),
    ( 9, 'Admin 2FA Bloqueado',    'cen_admin_bloqueado', 'ADMIN',            true,  'JBSWY3DPEHPK3PXP', true,  5, 'cen_admin_bloqueado@pascoa.local'),
    (10, 'Usuario Inativo',        'cen_inativo',         'FINANCEIRO',       false, NULL,               false, 0, 'cen_inativo@pascoa.local'),
    (11, 'Usuario Sem Email',      'cen_semmail',         'ATENDENTE',        true,  NULL,               false, 0, NULL),
    (12, 'Atendente 4 Falhas 2FA', 'cen_quase_bloqueado', 'ATENDENTE',        true,  'JBSWY3DPEHPK3PXP', true,  4, 'cen_quase@pascoa.local')
) AS v(n, nome, login, role, ativo, secret, ativado, falhas, email)
WHERE NOT EXISTS (SELECT 1 FROM usuarios u WHERE u.login = v.login);

-- 10 tokens de reset: valido x4, expirado x3, usado x2 (+ usuario sem email e inativo)
INSERT INTO password_reset_token (usuario_id, token, expira_em, usado, criado_em)
SELECT u.id, '00000ce0-0003-4000-8000-' || lpad(v.n::text, 12, '0'),
       now() + v.minutos * interval '1 minute', v.usado, now() - interval '5 minutes'
FROM (VALUES
    ( 1, 'cen_atendente',   20,   false),
    ( 2, 'cen_financeiro',  25,   false),
    ( 3, 'cen_confeiteiro', 10,   false),
    ( 4, 'cen_qualidade',  -60,   false),
    ( 5, 'cen_analista',   -600,  false),
    ( 6, 'cen_atendente',  -1440, false),
    ( 7, 'cen_financeiro',  20,   true),
    ( 8, 'cen_confeiteiro',-60,   true),
    ( 9, 'cen_semmail',     20,   false),
    (10, 'cen_inativo',     20,   false)
) AS v(n, login, minutos, usado)
JOIN usuarios u ON u.login = v.login
WHERE NOT EXISTS (SELECT 1 FROM password_reset_token t WHERE t.token = '00000ce0-0003-4000-8000-' || lpad(v.n::text, 12, '0'));

-- ---------------------------------------------------------------------------
-- 2. Fornecedores (CEN-FOR)
-- ---------------------------------------------------------------------------
INSERT INTO fornecedores (nome, cnpj, telefone, email, observacoes, criado_em, criado_por)
SELECT v.nome || ' [CEN-FOR-' || lpad(v.n::text, 2, '0') || ']', v.cnpj, v.tel, v.email, v.obs, now() - interval '200 days', 'seed'
FROM (VALUES
    ( 1, 'Fornecedor Completo',          '10.111.222/0001-01', '(11) 98100-0001', 'contato1@cen.local', 'Entrega em 2 dias'),
    ( 2, 'Fornecedor Sem CNPJ',          NULL,                 '(11) 98100-0002', 'contato2@cen.local', NULL),
    ( 3, 'Fornecedor Sem Telefone',      '10.111.222/0001-03', NULL,              'contato3@cen.local', NULL),
    ( 4, 'Fornecedor Sem Email',         '10.111.222/0001-04', '(11) 98100-0004', NULL,                NULL),
    ( 5, 'Fornecedor So Nome',           NULL,                 NULL,              NULL,                NULL),
    ( 6, 'Fornecedor Sem Materia-Prima', '10.111.222/0001-06', '(11) 98100-0006', 'contato6@cen.local', 'Nenhuma materia-prima vinculada'),
    ( 7, 'Fornecedor Observacao Longa',  '10.111.222/0001-07', '(11) 98100-0007', 'contato7@cen.local', repeat('Pedido minimo de 50kg; frete por conta do comprador. ', 8)),
    ( 8, 'Fornecedor Acentuacao Cacau',  '10.111.222/0001-08', '(11) 98100-0008', 'contato8@cen.local', 'Açúcar, côco e avelã'),
    ( 9, 'Fornecedor Pagamento Futuro',  '10.111.222/0001-09', '(11) 98100-0009', 'contato9@cen.local', 'Somente boleto 30 dias'),
    (10, 'Fornecedor Recente',           '10.111.222/0001-10', '(11) 98100-0010', 'contato10@cen.local','Cadastrado na safra atual')
) AS v(n, nome, cnpj, tel, email, obs)
WHERE NOT EXISTS (SELECT 1 FROM fornecedores f WHERE f.nome = v.nome || ' [CEN-FOR-' || lpad(v.n::text, 2, '0') || ']');

CREATE TEMP TABLE cen_for AS
SELECT id, substring(nome from 'CEN-FOR-([0-9]+)')::int AS n FROM fornecedores WHERE nome LIKE '%[CEN-FOR-%';

-- ---------------------------------------------------------------------------
-- 3. Materias-primas (CEN-MP): 6 unidades, critica, zerada, sem fornecedor, custo zero,
--    custo medio divergente, sem movimentacao, no limite do minimo, muita movimentacao
-- ---------------------------------------------------------------------------
INSERT INTO materias_primas (nome, unidade, quantidade_atual, quantidade_minima, custo_unitario,
                             custo_medio_ponderado, data_ultima_compra, fornecedor_preferencial_id, criado_em, criado_por)
SELECT v.nome || ' [CEN-MP-' || lpad(v.n::text, 2, '0') || ']', v.unidade, v.atual, v.minima, v.custo, v.medio,
       CURRENT_DATE - 10, f.id, now() - interval '120 days', 'seed'
FROM (VALUES
    ( 1, 'Chocolate ao Leite',   'KG', 0, 20.000, 42.0000, 42.0000,  1),
    ( 2, 'Cacau em Po',          'G',  0, 50.000,  0.0800,  0.0800,  2),
    ( 3, 'Leite Condensado',     'L',  0, 10.000, 12.9000, 12.9000,  1),
    ( 4, 'Essencia Baunilha',    'ML', 0, 20.000,  0.3500,  0.4100,  3),
    ( 5, 'Embalagem Ovo',        'UN', 0, 20.000,  2.1000,  2.1000,  4),
    ( 6, 'Caixa Presente',       'CX', 0, 10.000,  7.8000,  7.8000,  5),
    ( 7, 'Insumo Critico',       'KG', 0, 200.000, 48.5000, 48.5000, 1),
    ( 8, 'Insumo Zerado',        'KG', 0, 10.000, 51.0000, 51.0000,  2),
    ( 9, 'Insumo Sem Fornecedor','UN', 0, 20.000,  0.9000,  0.9000,  NULL),
    (10, 'Insumo Custo Zero',    'KG', 0, 20.000,  0.0000,  0.0000,  3),
    (11, 'Insumo Sem Movimento', 'KG', 5.000, 5.000, 33.7000, 40.2000, 1),
    (12, 'Insumo Muito Movido',  'UN', 0, 92.000,  1.2000,  1.2000,  2)
) AS v(n, nome, unidade, atual, minima, custo, medio, forn)
LEFT JOIN cen_for f ON f.n = v.forn
WHERE NOT EXISTS (SELECT 1 FROM materias_primas m WHERE m.nome = v.nome || ' [CEN-MP-' || lpad(v.n::text, 2, '0') || ']');

CREATE TEMP TABLE cen_mp AS
SELECT id, substring(nome from 'CEN-MP-([0-9]+)')::int AS n FROM materias_primas WHERE nome LIKE '%[CEN-MP-%';

-- Movimentacoes: ENTRADA 100, SAIDA 30, SAIDA 25, AJUSTE -5, AJUSTE +2, ENTRADA 50 => saldo 92.
-- MP 8 recebe SAIDA final de 92 (zerada); MP 12 recebe 60 pares ENTRADA/SAIDA (126 movs, saldo 92 = minimo exato).
INSERT INTO movimentacoes_estoque (materia_prima_id, tipo, quantidade, saldo_apos, data, motivo, usuario, custo_unitario)
SELECT mp_id, tipo, quantidade,
       SUM(delta) OVER (PARTITION BY mp_id ORDER BY k ROWS UNBOUNDED PRECEDING),
       data, motivo, usuario, custo
FROM (
    SELECT m.id AS mp_id, s.k, s.tipo, s.quantidade, s.delta,
           date_trunc('hour', now()) - interval '60 days' + s.k * interval '6 hours' AS data,
           s.motivo, 'seed' AS usuario,
           CASE WHEN s.tipo = 'ENTRADA' THEN mp.custo_unitario END AS custo
    FROM cen_mp m
    JOIN materias_primas mp ON mp.id = m.id
    CROSS JOIN (VALUES
        (1, 'ENTRADA', 100.000, 100.000, '[CEN-EST] Estoque inicial'),
        (2, 'SAIDA',    30.000, -30.000, '[CEN-EST] Producao | consumo do lote 1'),
        (3, 'SAIDA',    25.000, -25.000, '[CEN-EST] Producao | consumo do lote 2'),
        (4, 'AJUSTE',   -5.000,  -5.000, '[CEN-EST] Ajuste de inventario (perda)'),
        (5, 'AJUSTE',    2.000,   2.000, '[CEN-EST] Ajuste de inventario (sobra)'),
        (6, 'ENTRADA',  50.000,  50.000, '[CEN-EST] Compra de reposicao NF 9001')
    ) AS s(k, tipo, quantidade, delta, motivo)
    WHERE m.n <> 11 AND NOT EXISTS (SELECT 1 FROM movimentacoes_estoque x WHERE x.materia_prima_id = m.id)
    UNION ALL
    SELECT m.id, 7, 'SAIDA', 92.000, -92.000, date_trunc('hour', now()) - interval '60 days' + 7 * interval '6 hours',
           '[CEN-EST] Saida total (estoque zerado)', 'seed', NULL
    FROM cen_mp m
    WHERE m.n = 8 AND NOT EXISTS (SELECT 1 FROM movimentacoes_estoque x WHERE x.materia_prima_id = m.id)
    UNION ALL
    SELECT m.id, 100 + e.j * 2 + e.t, CASE WHEN e.t = 0 THEN 'ENTRADA' ELSE 'SAIDA' END, 10.000,
           CASE WHEN e.t = 0 THEN 10.000 ELSE -10.000 END,
           date_trunc('hour', now()) - interval '60 days' + (100 + e.j * 2 + e.t) * interval '6 hours',
           '[CEN-EST] Giro rapido ' || e.j, 'seed',
           CASE WHEN e.t = 0 THEN 1.2000 END
    FROM cen_mp m
    CROSS JOIN generate_series(0, 59) AS e0(j)
    CROSS JOIN generate_series(0, 1) AS e1(t)
    CROSS JOIN LATERAL (SELECT e0.j AS j, e1.t AS t) e
    WHERE m.n = 12 AND NOT EXISTS (SELECT 1 FROM movimentacoes_estoque x WHERE x.materia_prima_id = m.id)
) base;

UPDATE materias_primas m
SET quantidade_atual = s.saldo,
    data_ultima_compra = s.compra
FROM (
    SELECT DISTINCT ON (e.materia_prima_id) e.materia_prima_id, e.saldo_apos AS saldo,
           (SELECT MAX(x.data)::date FROM movimentacoes_estoque x
             WHERE x.materia_prima_id = e.materia_prima_id AND x.tipo = 'ENTRADA') AS compra
    FROM movimentacoes_estoque e
    JOIN cen_mp c ON c.id = e.materia_prima_id
    ORDER BY e.materia_prima_id, e.data DESC, e.id DESC
) s
WHERE s.materia_prima_id = m.id;

-- ---------------------------------------------------------------------------
-- 4. Produtos (CEN-PRD) e fichas tecnicas (CEN-FT)
-- ---------------------------------------------------------------------------
INSERT INTO produtos (nome, descricao, categoria_id, preco_venda, ativo, margem_desejada, inicio_safra, fim_safra, excluido_em, criado_em, criado_por)
SELECT v.nome || ' [CEN-PRD-' || lpad(v.n::text, 2, '0') || ']', v.descricao, (SELECT cp.id FROM categorias_produto cp WHERE cp.loja_id = 1 AND upper(cp.nome) = v.categoria), v.preco, v.ativo, v.margem,
       v.ini, v.fim, v.excluido, now() - interval '150 days', 'seed'
FROM (VALUES
    ( 1, 'Ovo Trufado Padrao',      'Ovo trufado cenario',     'TRUFADO',     89.90, true,  45.00, NULL::date, NULL::date, NULL::timestamp),
    ( 2, 'Ovo Recheado Padrao',     'Ovo recheado cenario',    'RECHEADO',   119.90, true,  45.00, NULL, NULL, NULL),
    ( 3, 'Ovo Diet (MP zerada)',    'Usa insumo sem estoque',  'DIET',        74.90, true,  40.00, NULL, NULL, NULL),
    ( 4, 'Ovo Vegano',              'Ovo vegano cenario',      'VEGANO',      99.90, true,  50.00, NULL, NULL, NULL),
    ( 5, 'Ovo Tradicional Sem Margem', NULL,                   'TRADICIONAL', 59.90, true,  NULL,  NULL, NULL, NULL),
    ( 6, 'Ovo Especial',            'Ovo especial cenario',    'ESPECIAL',    94.90, true,  55.00, NULL, NULL, NULL),
    ( 7, 'Ovo Inativo',             'Produto desativado',      'TRADICIONAL', 64.90, false, 40.00, NULL, NULL, NULL),
    ( 8, 'Ovo Safra Vigente',       'Safra em andamento',      'ESPECIAL',   109.90, true,  45.00, CURRENT_DATE - 30, CURRENT_DATE + 60, NULL),
    ( 9, 'Ovo Safra Encerrada',     'Safra encerrada',         'RECHEADO',   104.90, true,  45.00, CURRENT_DATE - 120, CURRENT_DATE - 30, NULL),
    (10, 'Ovo Excluido',            'Soft-delete',             'TRUFADO',     79.90, true,  45.00, NULL, NULL, now() - interval '10 days'),
    (11, 'Ovo Ficha Sem Itens',     'Ficha tecnica vazia',     'TRADICIONAL', 69.90, true,  45.00, NULL, NULL, NULL),
    (12, 'Ovo Rendimento Zero',     'Ficha com rendimento 0',  'RECHEADO',    84.90, true,  45.00, NULL, NULL, NULL),
    (13, 'Ovo Ficha 10 Itens',      'Ficha com 10 ingredientes','ESPECIAL',  139.90, true,  45.00, NULL, NULL, NULL),
    (14, 'Ovo Sem Ficha Tecnica',   'Sem ficha cadastrada',    'TRUFADO',     72.90, true,  45.00, NULL, NULL, NULL)
) AS v(n, nome, descricao, categoria, preco, ativo, margem, ini, fim, excluido)
WHERE NOT EXISTS (SELECT 1 FROM produtos p WHERE p.nome = v.nome || ' [CEN-PRD-' || lpad(v.n::text, 2, '0') || ']');

CREATE TEMP TABLE cen_prd AS
SELECT id, substring(nome from 'CEN-PRD-([0-9]+)')::int AS n FROM produtos WHERE nome LIKE '%[CEN-PRD-%';

INSERT INTO fichas_tecnicas (produto_id, rendimento, unidade_rendimento, observacoes)
SELECT p.id, CASE WHEN p.n = 12 THEN 0.000 ELSE 1.000 + (p.n % 3) END, 'UN', '[CEN-FT] ficha do produto ' || p.n
FROM cen_prd p
WHERE p.n BETWEEN 1 AND 13 AND NOT EXISTS (SELECT 1 FROM fichas_tecnicas f WHERE f.produto_id = p.id);

INSERT INTO fichas_tecnicas_itens (ficha_tecnica_id, materia_prima_id, quantidade)
SELECT f.id, m.id, v.qtd
FROM (VALUES
    ( 1, 1, 0.250), ( 1, 5, 1.000),
    ( 2, 1, 0.300), ( 2, 3, 0.100), ( 2, 5, 1.000),
    ( 3, 8, 0.350), ( 3, 5, 1.000),
    ( 4, 1, 0.350), ( 4, 4, 5.000),
    ( 5, 1, 0.250),
    ( 6, 2, 40.000), ( 6, 6, 1.000), ( 6, 1, 0.200),
    ( 7, 1, 0.250),
    ( 8, 7, 0.300),
    ( 9, 9, 2.000), ( 9, 1, 0.200),
    (10, 1, 0.100),
    (12, 1, 0.200), (12, 5, 1.000),
    (13, 1, 0.050), (13, 2, 0.050), (13, 3, 0.050), (13, 4, 0.050), (13, 5, 0.050),
    (13, 6, 0.050), (13, 7, 0.050), (13, 8, 0.050), (13, 9, 0.050), (13, 10, 0.050)
) AS v(prd, mp, qtd)
JOIN cen_prd p ON p.n = v.prd
JOIN fichas_tecnicas f ON f.produto_id = p.id
JOIN cen_mp m ON m.n = v.mp
ON CONFLICT (ficha_tecnica_id, materia_prima_id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 5. Clientes (CEN-CLI): canais, opt-in, segmentos, aniversario, LGPD, soft-delete
-- ---------------------------------------------------------------------------
INSERT INTO clientes (nome, telefone, email, endereco, cpf, excluido_em, preferencia_canal, opt_in, data_cadastro,
                      data_consentimento, anonimizado, segmento, data_nascimento, criado_por)
SELECT v.nome || ' [CEN-CLI-' || lpad(v.n::text, 2, '0') || ']', v.tel, v.email, v.endereco, v.cpf,
       CASE WHEN v.excluido THEN now() - interval '10 days' END,
       v.canal, v.opt, now() - interval '200 days',
       CASE WHEN v.opt THEN now() - interval '200 days' END,
       v.anon, v.segmento, v.nasc, 'seed'
FROM (VALUES
    ( 1, 'Sem Contato Nenhum',          NULL,              NULL,                  NULL,                    NULL,          'NENHUM',   false, 'NOVO',    NULL::date, false, false),
    ( 2, 'WhatsApp Opt-in',             '(11) 96000-0002', 'cen02@teste.local',   'Rua Cenario, 2',        '20000000002', 'WHATSAPP', true,  'REGULAR', NULL, false, false),
    ( 3, 'Email Opt-in',                '(11) 96000-0003', 'cen03@teste.local',   'Rua Cenario, 3',        '20000000003', 'EMAIL',    true,  'REGULAR', NULL, false, false),
    ( 4, 'Ambos Opt-in',                '(11) 96000-0004', 'cen04@teste.local',   'Rua Cenario, 4',        '20000000004', 'AMBOS',    true,  'REGULAR', NULL, false, false),
    ( 5, 'Canal Nenhum',                '(11) 96000-0005', 'cen05@teste.local',   'Rua Cenario, 5',        '20000000005', 'NENHUM',   false, 'NOVO',    NULL, false, false),
    ( 6, 'WhatsApp Opt-out',            '(11) 96000-0006', 'cen06@teste.local',   'Rua Cenario, 6',        '20000000006', 'WHATSAPP', false, 'REGULAR', NULL, false, false),
    ( 7, 'Sem Telefone WhatsApp',       NULL,              'cen07@teste.local',   'Rua Cenario, 7',        '20000000007', 'WHATSAPP', true,  'NOVO',    NULL, false, false),
    ( 8, 'Sem Email Canal Email',       '(11) 96000-0008', NULL,                  'Rua Cenario, 8',        '20000000008', 'EMAIL',    true,  'NOVO',    NULL, false, false),
    ( 9, 'VIP LTV Alto',                '(11) 96000-0009', 'cen09@teste.local',   'Rua Cenario, 9',        '20000000009', 'AMBOS',    true,  'VIP',     NULL, false, false),
    (10, 'Regular Uma Compra',          '(11) 96000-0010', 'cen10@teste.local',   'Rua Cenario, 10',       '20000000010', 'WHATSAPP', true,  'REGULAR', NULL, false, false),
    (11, 'Inativo 150 Dias',            '(11) 96000-0011', 'cen11@teste.local',   'Rua Cenario, 11',       '20000000011', 'EMAIL',    true,  'INATIVO', NULL, false, false),
    (12, 'Novo Sem Pedidos',            '(11) 96000-0012', 'cen12@teste.local',   'Rua Cenario, 12',       '20000000012', 'WHATSAPP', true,  'NOVO',    NULL, false, false),
    (13, 'Aniversario Hoje Opt-in',     '(11) 96000-0013', 'cen13@teste.local',   'Rua Cenario, 13',       '20000000013', 'WHATSAPP', true,  'REGULAR',
         make_date(1992, EXTRACT(MONTH FROM CURRENT_DATE)::int, EXTRACT(DAY FROM CURRENT_DATE)::int), false, false),
    (14, 'Aniversario Hoje Opt-out',    '(11) 96000-0014', 'cen14@teste.local',   'Rua Cenario, 14',       '20000000014', 'WHATSAPP', false, 'REGULAR',
         make_date(1992, EXTRACT(MONTH FROM CURRENT_DATE)::int, EXTRACT(DAY FROM CURRENT_DATE)::int), false, false),
    (15, 'Aniversario Amanha',          '(11) 96000-0015', 'cen15@teste.local',   'Rua Cenario, 15',       '20000000015', 'EMAIL',    true,  'REGULAR',
         ((CURRENT_DATE + 1) - interval '30 years')::date, false, false),
    (16, 'Titular Anonimo',             NULL,              NULL,                  NULL,                    NULL,          'NENHUM',   false, 'NOVO',    NULL, false, true),
    (17, 'Excluido Soft-delete',        '(11) 96000-0017', 'cen17@teste.local',   'Rua Cenario, 17',       '20000000017', 'EMAIL',    true,  'REGULAR', NULL, true,  false),
    (18, 'Zélia Açaí Cenário',          '(11) 96000-0018', 'zelia@teste.local',   'Rua João Pessoa, 18',   '20000000018', 'AMBOS',    true,  'REGULAR', NULL, false, false),
    (19, 'VIP Tres Pedidos',            '(11) 96000-0019', 'cen19@teste.local',   'Rua Cenario, 19',       '20000000019', 'AMBOS',    true,  'VIP',     NULL, false, false),
    (20, 'Sem Endereco Nem CPF',        '(11) 96000-0020', 'cen20@teste.local',   NULL,                    NULL,          'EMAIL',    true,  'NOVO',    NULL, false, false),
    (21, 'Nascido Em 31 de Dezembro',   '(11) 96000-0021', 'cen21@teste.local',   'Rua Cenario, 21',       '20000000021', 'WHATSAPP', true,  'REGULAR', DATE '1988-12-31', false, false)
) AS v(n, nome, tel, email, endereco, cpf, canal, opt, segmento, nasc, excluido, anon)
WHERE NOT EXISTS (SELECT 1 FROM clientes c WHERE c.nome = v.nome || ' [CEN-CLI-' || lpad(v.n::text, 2, '0') || ']');

-- 13 clientes de ranking (CRM ranking limita em 20; com os acima passam de 20 com pedidos)
INSERT INTO clientes (nome, telefone, email, endereco, cpf, preferencia_canal, opt_in, data_cadastro, data_consentimento,
                      anonimizado, segmento, criado_por)
SELECT 'Ranking CRM ' || g || ' [CEN-CLI-' || lpad(g::text, 2, '0') || ']',
       '(11) 96100-' || lpad(g::text, 4, '0'), 'ranking' || g || '@teste.local', 'Av. Ranking, ' || g,
       '30000000' || lpad(g::text, 3, '0'), (ARRAY['WHATSAPP','EMAIL','AMBOS'])[1 + g % 3], true,
       now() - interval '100 days', now() - interval '100 days', false, 'REGULAR', 'seed'
FROM generate_series(22, 34) g
WHERE NOT EXISTS (SELECT 1 FROM clientes c WHERE c.nome = 'Ranking CRM ' || g || ' [CEN-CLI-' || lpad(g::text, 2, '0') || ']');

CREATE TEMP TABLE cen_cli AS
SELECT id, substring(nome from 'CEN-CLI-([0-9]+)')::int AS n FROM clientes WHERE nome LIKE '%[CEN-CLI-%';

-- ---------------------------------------------------------------------------
-- 6. Pedidos (CEN-PED): 25 roteirizados + 55 CONFIRMADO + 12 CANCELADO + 52 EM_PRODUCAO + 60 ENTREGUE (paginacao/kanban)
-- ---------------------------------------------------------------------------
INSERT INTO pedidos (cliente_id, data_pedido, data_entrega, status, observacoes, total_pedido, token_acompanhamento, slot_entrega)
SELECT c.id, now() - v.dias * interval '1 day', CASE WHEN v.entrega IS NOT NULL THEN CURRENT_DATE + v.entrega END,
       v.status, '[CEN-PED-' || lpad(v.n::text, 2, '0') || '] ' || v.obs, 0,
       '00000ce0-0001-4000-8000-' || lpad(v.n::text, 12, '0'),
       CASE WHEN v.entrega IS NOT NULL THEN make_time(8 + v.n % 9, 0, 0) END
FROM (VALUES
    ( 1,  2, 'NOVO',        3,   10,   'NOVO sem itens'),
    ( 2,  3, 'NOVO',        2,    7,   'NOVO com itens, sem snapshot de custo'),
    ( 3,  4, 'NOVO',        6,   -2,   'NOVO com entrega no passado'),
    ( 4,  2, 'CONFIRMADO',  3,    5,   'CONFIRMADO com sinal'),
    ( 5,  5, 'CONFIRMADO',  9,   -3,   'CONFIRMADO atrasado sem pagamento'),
    ( 6,  4, 'EM_PRODUCAO', 4,    2,   'EM_PRODUCAO com ordens mistas'),
    ( 7,  6, 'EM_PRODUCAO', 3,    0,   'EM_PRODUCAO entrega hoje, sinal duplicado'),
    ( 8,  7, 'PRONTO',      5,    0,   'PRONTO hoje quitado'),
    ( 9,  8, 'PRONTO',      6,    1,   'PRONTO com sinal'),
    (10,  9, 'ENTREGUE',   14,  -10,   'ENTREGUE quitado PIX'),
    (11,  9, 'ENTREGUE',   35,  -30,   'ENTREGUE quitado em 3 parcelas'),
    (12,  9, 'ENTREGUE',   65,  -60,   'ENTREGUE aging 31-60 FIADO'),
    (13,  9, 'ENTREGUE',   95,  -90,   'ENTREGUE aging 61-90'),
    (14, 10, 'ENTREGUE',   25,  -20,   'ENTREGUE aging 1-30 saldo 30%'),
    (15, 11, 'ENTREGUE',  155, -150,   'ENTREGUE aging >90'),
    (16,  5, 'CANCELADO',  12,   -3,   'CANCELADO sem pagamento'),
    (17,  6, 'CANCELADO',  12,   -3,   'CANCELADO com pagamento (devolver)'),
    (18, 19, 'CONFIRMADO',  3, NULL,   'CONFIRMADO sem data de entrega'),
    (19, 19, 'CONFIRMADO',  2,    2,   'CONFIRMADO entrega em 2 dias (aging corrente)'),
    (20, 19, 'ENTREGUE',   10,   -5,   'ENTREGUE quitado (VIP 3 pedidos)'),
    (21, 19, 'ENTREGUE',   17,  -12,   'ENTREGUE quitado (VIP 3 pedidos)'),
    (22, 19, 'ENTREGUE',   30,  -25,   'ENTREGUE quitado (VIP 3 pedidos)'),
    (23, 17, 'ENTREGUE',   45,  -40,   'ENTREGUE de cliente soft-deleted'),
    (24, 16, 'ENTREGUE',   40,  -35,   'ENTREGUE de cliente anonimizado'),
    (25, 10, 'NOVO',        1,    6,   'NOVO originado de orcamento convertido')
) AS v(n, cli, status, dias, entrega, obs)
JOIN cen_cli c ON c.n = v.cli
WHERE NOT EXISTS (SELECT 1 FROM pedidos p WHERE p.token_acompanhamento = '00000ce0-0001-4000-8000-' || lpad(v.n::text, 12, '0'));

INSERT INTO pedidos (cliente_id, data_pedido, data_entrega, status, observacoes, total_pedido, token_acompanhamento, slot_entrega)
SELECT c.id, now() - b.dias * interval '1 day', CURRENT_DATE + b.entrega, b.status,
       '[CEN-PED-' || lpad(b.n::text, 2, '0') || '] volume para paginacao/kanban', 0,
       '00000ce0-0001-4000-8000-' || lpad(b.n::text, 12, '0'), make_time(8 + b.n % 9, 0, 0)
FROM (
    SELECT g AS n, 'CONFIRMADO' AS status, 2 AS dias, 3 + g % 5 AS entrega FROM generate_series(101, 155) g
    UNION ALL SELECT g, 'CANCELADO',   20, -(5 + g % 5)  FROM generate_series(201, 212) g
    UNION ALL SELECT g, 'EM_PRODUCAO',  4, 1 + g % 4     FROM generate_series(301, 352) g
    UNION ALL SELECT g, 'ENTREGUE', 30 + g % 40, -(5 + g % 30) FROM generate_series(401, 460) g
) b
JOIN cen_cli c ON c.n = 2 + b.n % 7
WHERE NOT EXISTS (SELECT 1 FROM pedidos p WHERE p.token_acompanhamento = '00000ce0-0001-4000-8000-' || lpad(b.n::text, 12, '0'));

CREATE TEMP TABLE cen_ped AS
SELECT id, right(token_acompanhamento, 12)::bigint::int AS n
FROM pedidos WHERE token_acompanhamento LIKE '00000ce0-0001-%';

-- Itens (pedido 1 fica sem itens de proposito)
INSERT INTO itens_pedido (pedido_id, produto_id, quantidade, preco_unitario, subtotal, custo_unitario)
SELECT p.id, pr.id, v.qtd, pr.preco_venda, pr.preco_venda * v.qtd,
       CASE WHEN ped.status IN ('NOVO', 'CANCELADO') THEN NULL ELSE round(pr.preco_venda * 0.45, 2) END
FROM (VALUES
    ( 2, 1, 2), ( 2, 2, 1), ( 3, 5, 1), ( 4, 2, 1), ( 5, 1, 2),
    ( 6, 1, 1), ( 6, 2, 1), ( 6, 6, 2), ( 7, 4, 2), ( 7, 5, 1),
    ( 8, 6, 1), ( 9, 1, 1), (10, 1, 2), (11, 2, 3), (11, 5, 1),
    (12, 6, 2), (13, 3, 1), (14, 4, 2), (15, 1, 1), (16, 2, 1),
    (17, 6, 1), (18, 1, 1), (19, 5, 2), (20, 1, 1), (21, 2, 1),
    (22, 4, 1), (23, 1, 1), (24, 2, 1), (25, 3, 1), (25, 4, 1)
) AS v(pedn, prd, qtd)
JOIN cen_ped p ON p.n = v.pedn
JOIN pedidos ped ON ped.id = p.id
JOIN cen_prd cp ON cp.n = v.prd
JOIN produtos pr ON pr.id = cp.id
WHERE NOT EXISTS (SELECT 1 FROM itens_pedido i WHERE i.pedido_id = p.id);

INSERT INTO itens_pedido (pedido_id, produto_id, quantidade, preco_unitario, subtotal, custo_unitario)
SELECT p.id, pr.id, 1 + p.n % 3, pr.preco_venda, pr.preco_venda * (1 + p.n % 3),
       CASE WHEN ped.status = 'CANCELADO' THEN NULL ELSE round(pr.preco_venda * 0.45, 2) END
FROM cen_ped p
JOIN pedidos ped ON ped.id = p.id
JOIN cen_prd cp ON cp.n = 1 + p.n % 6
JOIN produtos pr ON pr.id = cp.id
WHERE p.n >= 101 AND NOT EXISTS (SELECT 1 FROM itens_pedido i WHERE i.pedido_id = p.id);

UPDATE pedidos p
SET total_pedido = t.total
FROM (SELECT i.pedido_id, SUM(i.subtotal) AS total FROM itens_pedido i JOIN cen_ped c ON c.id = i.pedido_id GROUP BY i.pedido_id) t
WHERE t.pedido_id = p.id;

-- ---------------------------------------------------------------------------
-- 7. Pagamentos (CEN-PAG): 5 formas, parcial, quitado, 3 parcelas, duplicata suja, pedido cancelado pago
--    valor truncado em centavos para a soma nunca passar do total
-- ---------------------------------------------------------------------------
INSERT INTO pagamentos (pedido_id, valor, tipo_pagamento, data_pagamento, observacoes)
SELECT p.id, trunc(ped.total_pedido * v.pct, 2), v.tipo, CURRENT_DATE - v.dias, '[CEN-PAG] ' || v.obs
FROM (
    SELECT * FROM (VALUES
        ( 4, 0.50, 'PIX',            2, 'sinal 50%'),
        ( 6, 0.30, 'PIX',            3, 'sinal 30%'),
        ( 7, 0.20, 'PIX',            2, 'sinal'),
        ( 7, 0.20, 'PIX',            2, 'sinal (duplicata suja)'),
        ( 8, 1.00, 'CARTAO_DEBITO',  1, 'quitado'),
        ( 9, 0.50, 'DINHEIRO',       4, 'sinal 50%'),
        (10, 1.00, 'PIX',           10, 'quitado'),
        (11, 0.40, 'DINHEIRO',      33, 'parcela 1/3'),
        (11, 0.30, 'CARTAO_CREDITO',31, 'parcela 2/3'),
        (11, 0.30, 'CARTAO_DEBITO', 30, 'parcela 3/3'),
        (12, 0.50, 'FIADO',         60, 'fiado 50%'),
        (13, 0.20, 'CARTAO_CREDITO',90, 'entrada 20%'),
        (14, 0.70, 'PIX',           20, 'pago 70%, saldo 30%'),
        (15, 0.20, 'DINHEIRO',     150, 'entrada 20%'),
        (17, 0.30, 'PIX',           10, 'sinal antes do cancelamento'),
        (18, 0.10, 'FIADO',          2, 'sinal 10%'),
        (20, 1.00, 'PIX',            5, 'quitado'),
        (21, 1.00, 'DINHEIRO',      12, 'quitado'),
        (22, 1.00, 'CARTAO_CREDITO',25, 'quitado'),
        (23, 1.00, 'PIX',           40, 'quitado'),
        (24, 1.00, 'PIX',           35, 'quitado')
    ) AS s(pedn, pct, tipo, dias, obs)
    UNION ALL
    SELECT n, 1.00, 'PIX', 5, 'quitado (volume)' FROM cen_ped WHERE n BETWEEN 401 AND 460
) v
JOIN cen_ped p ON p.n = v.pedn
JOIN pedidos ped ON ped.id = p.id
WHERE NOT EXISTS (SELECT 1 FROM pagamentos g WHERE g.observacoes LIKE '[CEN-PAG]%');

-- ---------------------------------------------------------------------------
-- 8. Orcamentos (CEN-ORC): 14 roteirizados + 55 PENDENTE (paginacao)
-- ---------------------------------------------------------------------------
INSERT INTO orcamentos (cliente_id, data_criacao, validade, status, total, observacoes, token_aprovacao, pedido_id, criado_em, criado_por)
SELECT c.id, now() - interval '3 days', CURRENT_DATE + v.validade, v.status, 0,
       '[CEN-ORC-' || lpad(v.n::text, 2, '0') || '] ' || v.obs,
       '00000ce0-0002-4000-8000-' || lpad(v.n::text, 12, '0'), p.id, now(), 'seed'
FROM (VALUES
    ( 1,  2, 'PENDENTE',  10, 'PENDENTE valido',                NULL::int),
    ( 2,  3, 'PENDENTE',   2, 'PENDENTE vence em 2 dias (job ORCAMENTO_EXPIRANDO)', NULL),
    ( 3,  4, 'PENDENTE',  -3, 'PENDENTE vencido (EXPIRADO sob demanda)', NULL),
    ( 4,  5, 'APROVADO',  10, 'APROVADO valido nao convertido', NULL),
    ( 5,  6, 'APROVADO',  -4, 'APROVADO vencido',               NULL),
    ( 6, 10, 'APROVADO',   5, 'APROVADO ja convertido em pedido', 25),
    ( 7,  7, 'RECUSADO',   8, 'RECUSADO pelo cliente',          NULL),
    ( 8,  8, 'EXPIRADO',  -20, 'EXPIRADO gravado',              NULL),
    ( 9,  2, 'PENDENTE',   6, 'PENDENTE sem itens',             NULL),
    (10,  3, 'PENDENTE',  15, 'PENDENTE com 8 itens',           NULL),
    (11,  4, 'PENDENTE',   1, 'PENDENTE vence amanha',          NULL),
    (12,  5, 'PENDENTE',   0, 'PENDENTE vence hoje',            NULL),
    (13,  6, 'APROVADO',   0, 'APROVADO vence hoje',            NULL),
    (14,  7, 'RECUSADO', -10, 'RECUSADO e vencido',             NULL)
) AS v(n, cli, status, validade, obs, pedn)
JOIN cen_cli c ON c.n = v.cli
LEFT JOIN cen_ped p ON p.n = v.pedn
WHERE NOT EXISTS (SELECT 1 FROM orcamentos o WHERE o.token_aprovacao = '00000ce0-0002-4000-8000-' || lpad(v.n::text, 12, '0'));

INSERT INTO orcamentos (cliente_id, data_criacao, validade, status, total, observacoes, token_aprovacao, criado_em, criado_por)
SELECT c.id, now() - interval '2 days', CURRENT_DATE + 5 + g % 20, 'PENDENTE', 0,
       '[CEN-ORC-' || lpad(g::text, 2, '0') || '] volume para paginacao',
       '00000ce0-0002-4000-8000-' || lpad(g::text, 12, '0'), now(), 'seed'
FROM generate_series(101, 155) g
JOIN cen_cli c ON c.n = 2 + g % 7
WHERE NOT EXISTS (SELECT 1 FROM orcamentos o WHERE o.token_aprovacao = '00000ce0-0002-4000-8000-' || lpad(g::text, 12, '0'));

CREATE TEMP TABLE cen_orc AS
SELECT id, right(token_aprovacao, 12)::bigint::int AS n
FROM orcamentos WHERE token_aprovacao LIKE '00000ce0-0002-%';

INSERT INTO orcamento_itens (orcamento_id, produto_id, quantidade, preco_unitario, subtotal)
SELECT o.id, pr.id, v.qtd, pr.preco_venda, pr.preco_venda * v.qtd
FROM (
    SELECT * FROM (VALUES
        ( 1, 1, 2), ( 1, 2, 1), ( 2, 3, 1), ( 3, 4, 1), ( 4, 1, 1), ( 4, 5, 2),
        ( 5, 6, 1), ( 6, 3, 1), ( 6, 4, 1), ( 7, 2, 1), ( 8, 5, 1),
        (10, 1, 1), (10, 2, 1), (10, 3, 1), (10, 4, 1), (10, 5, 1), (10, 6, 1), (10, 7, 1), (10, 8, 1),
        (11, 1, 1), (12, 2, 1), (13, 5, 1), (14, 1, 1)
    ) AS s(orcn, prd, qtd)
    UNION ALL
    SELECT n, 1 + n % 6, 1 + n % 3 FROM cen_orc WHERE n >= 101
) v
JOIN cen_orc o ON o.n = v.orcn
JOIN cen_prd cp ON cp.n = v.prd
JOIN produtos pr ON pr.id = cp.id
WHERE NOT EXISTS (SELECT 1 FROM orcamento_itens i WHERE i.orcamento_id = o.id);

UPDATE orcamentos o
SET total = t.total
FROM (SELECT i.orcamento_id, SUM(i.subtotal) AS total FROM orcamento_itens i JOIN cen_orc c ON c.id = i.orcamento_id GROUP BY i.orcamento_id) t
WHERE t.orcamento_id = o.id;

-- ---------------------------------------------------------------------------
-- 9. Producao (CEN-OP): derivada dos pedidos + 10 ordens avulsas (sem pedido, sem ficha, MP insuficiente, rendimento 0...)
--    EM_PRODUCAO 301..352 => 52 ordens EM_ANDAMENTO; PENDENTE 101..155 => 55; CANCELADA 201..212 => 12; CONCLUIDA 401..460 => 60+
-- ---------------------------------------------------------------------------
INSERT INTO ordens_producao (pedido_id, produto_id, quantidade, status, data_abertura, data_conclusao, observacoes)
SELECT x.pedido_id, x.produto_id, x.quantidade, x.st, x.data_abertura,
       CASE WHEN x.st = 'CONCLUIDA' THEN x.data_abertura + interval '2 days' END,
       '[CEN-OP] ordem do pedido ' || x.pn
FROM (
    SELECT ip.pedido_id, ip.produto_id, ip.quantidade, cp.n AS pn,
           CASE ped.status
               WHEN 'CONFIRMADO' THEN 'PENDENTE'
               WHEN 'CANCELADO'  THEN 'CANCELADA'
               WHEN 'EM_PRODUCAO' THEN
                   CASE WHEN cp.n >= 300 THEN 'EM_ANDAMENTO'
                        ELSE (CASE row_number() OVER (PARTITION BY ip.pedido_id ORDER BY ip.id)
                                  WHEN 1 THEN 'CONCLUIDA' WHEN 2 THEN 'EM_ANDAMENTO' ELSE 'PENDENTE' END) END
               ELSE 'CONCLUIDA'
           END AS st,
           ped.data_pedido + interval '6 hours' AS data_abertura
    FROM itens_pedido ip
    JOIN cen_ped cp ON cp.id = ip.pedido_id
    JOIN pedidos ped ON ped.id = ip.pedido_id
    WHERE ped.status <> 'NOVO'
      AND NOT EXISTS (SELECT 1 FROM ordens_producao o WHERE o.pedido_id = ip.pedido_id)
) x;

INSERT INTO ordens_producao (pedido_id, produto_id, quantidade, status, data_abertura, data_conclusao, observacoes)
SELECT NULL, cp.id, v.qtd, v.status, now() - v.dias * interval '1 day',
       CASE WHEN v.status = 'CONCLUIDA' THEN now() - (v.dias - 1) * interval '1 day' END,
       '[CEN-OP-' || lpad(v.n::text, 2, '0') || '] ' || v.obs
FROM (VALUES
    ( 1,  1, 5, 'PENDENTE',     2, 'avulsa sem pedido'),
    ( 2,  2, 3, 'EM_ANDAMENTO', 3, 'avulsa em andamento'),
    ( 3,  1, 2, 'CONCLUIDA',    5, 'avulsa concluida'),
    ( 4,  3, 4, 'PENDENTE',     1, 'MP insuficiente (concluir deve falhar)'),
    ( 5, 14, 1, 'PENDENTE',     1, 'produto sem ficha tecnica'),
    ( 6, 12, 1, 'PENDENTE',     1, 'ficha com rendimento zero'),
    ( 7, 11, 1, 'PENDENTE',     1, 'ficha sem itens'),
    ( 8,  4, 2, 'CANCELADA',    6, 'avulsa cancelada'),
    ( 9, 13, 1, 'PENDENTE',     1, 'ficha com 10 itens'),
    (10,  5, 6, 'EM_ANDAMENTO', 2, 'lote grande')
) AS v(n, prd, qtd, status, dias, obs)
JOIN cen_prd cp ON cp.n = v.prd
WHERE NOT EXISTS (SELECT 1 FROM ordens_producao o WHERE o.observacoes LIKE '[CEN-OP-' || lpad(v.n::text, 2, '0') || ']%');

-- ---------------------------------------------------------------------------
-- 10. Qualidade (CEN-QUA): checklist (itens inativos, produtos sem checklist) e inspecoes
-- ---------------------------------------------------------------------------
INSERT INTO checklist_qualidade (produto_id, item, ordem, ativo)
SELECT p.id,
       (ARRAY['Temperatura do chocolate','Brilho e acabamento','Peso conferido','Recheio centralizado',
              'Embalagem sem avaria','Rotulo com validade','Ausencia de manchas','Laco bem fixado'])[k],
       k, NOT (k = 4 AND p.n % 2 = 0)
FROM cen_prd p
CROSS JOIN generate_series(1, 8) k
WHERE p.n BETWEEN 1 AND 6 AND k <= 4 + p.n
  AND NOT EXISTS (SELECT 1 FROM checklist_qualidade c WHERE c.produto_id = p.id);

-- inspecao 1: ordens CONCLUIDA exceto ~1/9 (ordem sem inspecao); reprovada em ~1/5; inspetores distintos
INSERT INTO inspecao_qualidade (ordem_producao_id, data_inspecao, inspetor, aprovado, observacoes, itens_verificados)
SELECT o.id, COALESCE(o.data_conclusao, o.data_abertura) + interval '2 hours',
       (ARRAY['Diego Qualidade','Carla Confeiteira','Elisa Analista','cen_qualidade'])[1 + o.id % 4],
       o.id % 5 <> 0,
       CASE WHEN o.id % 5 = 0 THEN '[CEN-QUA] Reprovado: acabamento fora do padrao' ELSE '[CEN-QUA] Aprovado' END,
       (SELECT jsonb_agg(jsonb_build_object('checklistItemId', c.id, 'descricao', c.item,
                   'verificado', NOT (o.id % 5 = 0 AND c.ordem <= 2)) ORDER BY c.ordem)
          FROM checklist_qualidade c WHERE c.produto_id = o.produto_id AND c.ativo)
FROM ordens_producao o
WHERE o.status = 'CONCLUIDA' AND o.observacoes LIKE '[CEN-OP%' AND o.id % 9 <> 0
  AND NOT EXISTS (SELECT 1 FROM inspecao_qualidade i WHERE i.ordem_producao_id = o.id);

-- inspecao 2: reinspecao aprovada das reprovadas (varias inspecoes na mesma ordem)
INSERT INTO inspecao_qualidade (ordem_producao_id, data_inspecao, inspetor, aprovado, observacoes, itens_verificados)
SELECT o.id, COALESCE(o.data_conclusao, o.data_abertura) + interval '1 day', 'Diego Qualidade', true,
       '[CEN-QUA-2] Reinspecao aprovada apos retrabalho',
       (SELECT jsonb_agg(jsonb_build_object('checklistItemId', c.id, 'descricao', c.item, 'verificado', true) ORDER BY c.ordem)
          FROM checklist_qualidade c WHERE c.produto_id = o.produto_id AND c.ativo)
FROM ordens_producao o
WHERE o.status = 'CONCLUIDA' AND o.observacoes LIKE '[CEN-OP%' AND o.id % 5 = 0 AND o.id % 9 <> 0
  AND NOT EXISTS (SELECT 1 FROM inspecao_qualidade i WHERE i.ordem_producao_id = o.id AND i.observacoes LIKE '[CEN-QUA-2]%');

-- ---------------------------------------------------------------------------
-- 11. Financeiro (CEN-FIN)
-- ---------------------------------------------------------------------------
INSERT INTO configuracao_financeira (margem_desejada_padrao)
SELECT 30.00 WHERE NOT EXISTS (SELECT 1 FROM configuracao_financeira);

INSERT INTO contas_pagar (fornecedor_id, descricao, valor, vencimento, status, categoria)
SELECT f.id, '[CEN-FIN-' || lpad(v.n::text, 2, '0') || '] ' || v.descricao, v.valor, CURRENT_DATE + v.venc, v.status, v.cat
FROM (VALUES
    ( 1, 1,    'Chocolate (vencida ha 10 dias)',  1200.00,  -10, 'ABERTA',  'MATERIA_PRIMA'),
    ( 2, 2,    'Cacau (vence em 7 dias)',          800.00,    7, 'ABERTA',  'MATERIA_PRIMA'),
    ( 3, NULL, 'Energia (vence hoje)',             680.00,    0, 'ABERTA',  'DESPESA_FIXA'),
    ( 4, 3,    'Servico de entrega (paga)',        350.00,  -30, 'PAGA',    'SERVICO'),
    ( 5, NULL, 'Contador (vence em 30 dias)',      450.00,   30, 'ABERTA',  'SERVICO'),
    ( 6, 1,    'Chocolate (vencida ha 90 dias)',  2000.00,  -90, 'ABERTA',  'MATERIA_PRIMA'),
    ( 7, 4,    'Embalagens (vence em 3 dias)',     540.00,    3, 'ABERTA',  'OUTROS'),
    ( 8, NULL, 'Software (VENCIDA gravada)',        99.00,   -5, 'VENCIDA', 'SERVICO'),
    ( 9, 5,    'Frete (paga hoje)',                300.00,    0, 'PAGA',    'OUTROS'),
    (10, NULL, 'Aluguel (vence em 15 dias)',      2500.00,   15, 'ABERTA',  'DESPESA_FIXA'),
    (11, 2,    'Compra grande (vence em 45 dias)', 15000.00, 45, 'ABERTA',  'MATERIA_PRIMA'),
    (12, 6,    'Fornecedor sem MP (vencida 2 dias)',120.00,  -2, 'ABERTA',  'OUTROS')
) AS v(n, forn, descricao, valor, venc, status, cat)
LEFT JOIN cen_for f ON f.n = v.forn
WHERE NOT EXISTS (SELECT 1 FROM contas_pagar c WHERE c.descricao LIKE '[CEN-FIN-' || lpad(v.n::text, 2, '0') || ']%');

INSERT INTO despesas_fixas (descricao, valor, periodicidade, ativo, data_inicio, data_cancelamento)
SELECT '[CEN-FIN-DF-' || lpad(v.n::text, 2, '0') || '] ' || v.descricao, v.valor, v.period, v.ativo,
       CURRENT_DATE + v.inicio, CASE WHEN NOT v.ativo THEN CURRENT_DATE - 20 END
FROM (VALUES
    (1, 'Aluguel mensal',               2500.00, 'MENSAL',     true,  -300),
    (2, 'Seguro trimestral',             900.00, 'TRIMESTRAL', true,  -200),
    (3, 'Alvara semestral',              620.00, 'SEMESTRAL',  true,  -250),
    (4, 'Licenca anual',                1800.00, 'ANUAL',      true,  -100),
    (5, 'Telefone cancelado',             85.00, 'MENSAL',     false, -400),
    (6, 'Despesa de valor alto',       12000.00, 'ANUAL',      true,  -50),
    (7, 'Despesa que ainda vai iniciar',  300.00, 'MENSAL',    true,    30),
    (8, 'Marketing trimestral cancelado', 450.00, 'TRIMESTRAL',false, -180)
) AS v(n, descricao, valor, period, ativo, inicio)
WHERE NOT EXISTS (SELECT 1 FROM despesas_fixas d WHERE d.descricao LIKE '[CEN-FIN-DF-' || lpad(v.n::text, 2, '0') || ']%');

-- contas_receber legado, divergente de proposito (o aging deriva de pedidos - pagamentos e nao le esta tabela)
INSERT INTO contas_receber (pedido_id, valor_original, valor_pago, vencimento, status)
SELECT p.id, 999.00, 0, CURRENT_DATE - 200, 'ABERTA'
FROM cen_ped p
WHERE p.n IN (10, 11, 12) AND NOT EXISTS (SELECT 1 FROM contas_receber c WHERE c.pedido_id = p.id);

-- ---------------------------------------------------------------------------
-- 12. Gastos (CEN-GAS) e orcamentos de gasto (mes corrente e anterior)
-- ---------------------------------------------------------------------------
INSERT INTO gastos_variaveis (descricao, valor, data_lancamento, categoria, referencia_mes, referencia_ano,
                              observacoes, comprovante_url, criado_em, criado_por, desconsiderar_no_custo, pedido_id)
SELECT '[CEN-GAS-' || lpad(v.n::text, 2, '0') || '] ' || v.descricao, v.valor, d.dia, v.cat,
       EXTRACT(MONTH FROM d.dia)::int, EXTRACT(YEAR FROM d.dia)::int, v.obs, v.comp,
       d.dia::timestamp + interval '11 hours', 'financeiro', v.desconsiderar, p.id
FROM (VALUES
    ( 1, 'Sacolas personalizadas (estoura orcamento)', 450.00,  'EMBALAGEM',    'M0', NULL, NULL, false, NULL::int),
    ( 2, 'Combustivel da entrega',                     120.00,  'TRANSPORTE',   'M0', NULL, NULL, false, NULL),
    ( 3, 'Anuncio (categoria sem orcamento)',          300.00,  'MARKETING',    'M0', 'Campanha de Pascoa', NULL, false, NULL),
    ( 4, 'Chocolate avulso',                           800.00,  'MATERIA_PRIMA','M0', NULL, 'https://exemplo.local/comprovante-4.pdf', false, NULL),
    ( 5, 'Taxa diversa (estoura orcamento)',            90.00,  'OUTROS',       'M0', NULL, NULL, false, NULL),
    ( 6, 'Gasto desconsiderado no custo',               35.00,  'OUTROS',       'M0', 'Nao entra no custo', NULL, true, NULL),
    ( 7, 'Frete vinculado a pedido',                    60.00,  'TRANSPORTE',   'M0', NULL, NULL, false, 10),
    ( 8, 'Batedeira nova',                            1500.00,  'EQUIPAMENTO',  'M1', NULL, NULL, false, NULL),
    ( 9, 'Servico de design',                          400.00,  'SERVICO',      'M1', NULL, NULL, false, NULL),
    (10, 'Embalagens do mes anterior',                 200.00,  'EMBALAGEM',    'M1', NULL, NULL, false, NULL),
    (11, 'Marketing do mes anterior',                  250.00,  'MARKETING',    'M1', 'Impulsionamento', 'https://exemplo.local/comprovante-11.pdf', false, NULL),
    (12, 'Materia-prima do ano anterior',              950.00,  'MATERIA_PRIMA','Y1', NULL, NULL, false, NULL),
    (13, 'Gasto desconsiderado do mes anterior',        15.00,  'OUTROS',       'M1', NULL, NULL, true, NULL),
    (14, 'Equipamento de valor alto',                 5000.00,  'EQUIPAMENTO',  'M1', 'Forno industrial', NULL, false, NULL)
) AS v(n, descricao, valor, cat, quando, obs, comp, desconsiderar, pedn)
CROSS JOIN LATERAL (
    SELECT CASE v.quando
               WHEN 'M0' THEN date_trunc('month', CURRENT_DATE)::date
               WHEN 'M1' THEN (date_trunc('month', CURRENT_DATE) - interval '1 month' + interval '9 days')::date
               ELSE (CURRENT_DATE - interval '1 year')::date
           END AS dia
) d
LEFT JOIN cen_ped p ON p.n = v.pedn
WHERE NOT EXISTS (SELECT 1 FROM gastos_variaveis g WHERE g.descricao LIKE '[CEN-GAS-' || lpad(v.n::text, 2, '0') || ']%');

INSERT INTO orcamentos_gasto (categoria, valor_orcado, referencia_mes, referencia_ano)
SELECT v.cat, v.valor, EXTRACT(MONTH FROM m.dia)::int, EXTRACT(YEAR FROM m.dia)::int
FROM (VALUES
    ('EMBALAGEM', 100.00), ('TRANSPORTE', 800.00), ('MATERIA_PRIMA', 1000.00),
    ('EQUIPAMENTO', 300.00), ('SERVICO', 200.00), ('OUTROS', 50.00)
) AS v(cat, valor)
CROSS JOIN (VALUES (date_trunc('month', CURRENT_DATE)::date),
                   ((date_trunc('month', CURRENT_DATE) - interval '1 month')::date)) AS m(dia)
ON CONFLICT (loja_id, categoria, referencia_mes, referencia_ano) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 13. CRM (CEN-PTO pontos, CEN-NTA notas)
--     pontos sao sempre positivos; DEBITO e subtraido na consulta de saldo (ver PontoFidelidadeRepository)
-- ---------------------------------------------------------------------------
INSERT INTO pontos_fidelidade (cliente_id, pedido_id, pontos, tipo, descricao, data_operacao, data_expiracao)
SELECT c.id, NULL, v.pontos, v.tipo, '[CEN-PTO-' || lpad(v.n::text, 2, '0') || '] ' || v.descricao,
       now() - v.dias * interval '1 day', CASE WHEN v.exp IS NOT NULL THEN CURRENT_DATE + v.exp END
FROM (VALUES
    ( 1,  9, 300, 'CREDITO', 'Pontos acumulados',            60,  100),
    ( 2,  9,  50, 'DEBITO',  'Resgate de desconto',          20,  NULL),
    ( 3,  9, 100, 'CREDITO', 'Pontos expirados ontem',      400,   -1),
    ( 4,  9,  40, 'CREDITO', 'Expira hoje (ainda vale)',     30,    0),
    ( 5, 10,  20, 'CREDITO', 'Saldo baixo (DEBITO maior falha)', 20, 300),
    ( 6, 19, 150, 'CREDITO', 'Pontos acumulados',            12,  200),
    ( 7, 19,  30, 'DEBITO',  'Resgate',                       5,  NULL),
    ( 8, 11,  80, 'CREDITO', 'Tudo expirado (saldo zero)',  200,  -90),
    ( 9,  2,  10, 'CREDITO', 'Bonus de cadastro',            50,  300),
    (10,  3, 500, 'CREDITO', 'Saldo grande',                 10,  300)
) AS v(n, cli, pontos, tipo, descricao, dias, exp)
JOIN cen_cli c ON c.n = v.cli
WHERE NOT EXISTS (SELECT 1 FROM pontos_fidelidade x WHERE x.descricao LIKE '[CEN-PTO-' || lpad(v.n::text, 2, '0') || ']%');

INSERT INTO notas_cliente (cliente_id, texto, criado_em, criado_por)
SELECT c.id, '[CEN-NTA-' || lpad(v.n::text, 2, '0') || '] ' || v.texto, now() - v.n * interval '3 days', v.autor
FROM (VALUES
    ( 1,  9, 'Prefere retirar no balcao',             'cen_atendente'),
    ( 2,  9, 'Alergica a castanhas',                  'cen_atendente'),
    ( 3,  9, 'Sempre pede embalagem de presente',     'cen_admin'),
    ( 4, 10, 'Negocia desconto no volume',            'cen_atendente'),
    ( 5, 11, 'Cliente inativo, tentar reativar',      'cen_financeiro'),
    ( 6, 13, 'Aniversario hoje: oferecer cupom',      'cen_atendente'),
    ( 7, 19, 'Atrasa pagamento com frequencia',       'cen_financeiro'),
    ( 8, 18, 'Nota com acentuação: açaí, côco, avelã','cen_admin'),
    ( 9,  2, 'Pediu contato por WhatsApp apenas',     'cen_atendente'),
    (10,  3, repeat('Nota longa de atendimento. ', 40), 'cen_atendente')
) AS v(n, cli, texto, autor)
JOIN cen_cli c ON c.n = v.cli
WHERE NOT EXISTS (SELECT 1 FROM notas_cliente x WHERE x.texto LIKE '[CEN-NTA-' || lpad(v.n::text, 2, '0') || ']%');

-- ---------------------------------------------------------------------------
-- 14. Notificacoes (CEN-NOT) e alertas internos (CEN-ALE)
--     Nao altera configuracao_canal (ativo/test_mode) para nao mudar o comportamento global do ambiente.
-- ---------------------------------------------------------------------------
INSERT INTO templates_notificacao (evento_gatilho, canal, assunto, corpo, ativo, variaveis)
SELECT ev, cn, CASE WHEN cn = 'EMAIL' THEN 'Pascoa Artesanal - ' || replace(ev, '_', ' ') END,
       'Ola {{cliente}}, evento ' || ev || ' (pedido {{pedidoId}}, total {{total}}).', true, '{{cliente}},{{pedidoId}},{{total}},{{status}}'
FROM unnest(ARRAY['PEDIDO_CONFIRMADO','PRODUCAO_INICIADA','PEDIDO_PRONTO','PEDIDO_ENTREGUE','PAGAMENTO_RECEBIDO',
                  'PEDIDO_CANCELADO','ORCAMENTO_APROVADO','ORCAMENTO_RECUSADO','ANIVERSARIO_CLIENTE','ORCAMENTO_EXPIRANDO']) ev
CROSS JOIN unnest(ARRAY['EMAIL','WHATSAPP','SMS']) cn
WHERE NOT EXISTS (SELECT 1 FROM templates_notificacao t WHERE t.evento_gatilho = ev AND t.canal = cn);

INSERT INTO templates_notificacao (evento_gatilho, canal, assunto, corpo, ativo, variaveis)
SELECT 'PEDIDO_PRONTO', 'EMAIL', 'Template inativo', '[CEN-TPL] template inativo (o ativo continua sendo o usado)', false, '{{cliente}}'
WHERE NOT EXISTS (SELECT 1 FROM templates_notificacao t WHERE t.corpo LIKE '[CEN-TPL]%');

-- ENVIADA unica por (pedido, evento, canal) e por (orcamento, ORCAMENTO_EXPIRANDO, canal): respeita os indices parciais
INSERT INTO notificacoes_enviadas (pedido_id, template_id, canal, destinatario, data_envio, status, mensagem_erro, evento, cliente_id, orcamento_id)
SELECT p.id,
       (SELECT t.id FROM templates_notificacao t WHERE t.evento_gatilho = v.evento AND t.canal = v.canal AND t.ativo ORDER BY t.id LIMIT 1),
       v.canal,
       CASE WHEN v.canal = 'EMAIL' THEN 'cen.notif.' || lpad(v.n::text, 2, '0') || '@teste.local'
            ELSE '(11) 95555-' || lpad(v.n::text, 4, '0') END,
       now() - v.n * interval '1 day', v.status, v.erro, v.evento, c.id, o.id
FROM (VALUES
    ( 1,  4, NULL::int, NULL::int, 2, 'PEDIDO_CONFIRMADO',   'EMAIL',    'ENVIADA', NULL::text),
    ( 2,  4, NULL, NULL, 2, 'PEDIDO_CONFIRMADO',   'WHATSAPP', 'ENVIADA', NULL),
    ( 3,  6, NULL, NULL, 4, 'PRODUCAO_INICIADA',   'WHATSAPP', 'ENVIADA', NULL),
    ( 4,  8, NULL, NULL, 7, 'PEDIDO_PRONTO',       'EMAIL',    'ENVIADA', NULL),
    ( 5, 10, NULL, NULL, 9, 'PEDIDO_ENTREGUE',     'EMAIL',    'ENVIADA', NULL),
    ( 6, 10, NULL, NULL, 9, 'PAGAMENTO_RECEBIDO',  'WHATSAPP', 'ENVIADA', NULL),
    ( 7, 17, NULL, NULL, 6, 'PEDIDO_CANCELADO',    'EMAIL',    'ENVIADA', NULL),
    ( 8,  9, NULL, NULL, 8, 'PEDIDO_PRONTO',       'WHATSAPP', 'FALHA',   'WhatsApp API indisponivel (HTTP 503)'),
    ( 9,  9, NULL, NULL, 8, 'PEDIDO_PRONTO',       'SMS',      'ENVIADA', NULL),
    (10, 11, NULL, NULL, 9, 'PEDIDO_ENTREGUE',     'WHATSAPP', 'FALHA',   'Numero de telefone invalido'),
    (11, 11, NULL, NULL, 9, 'PEDIDO_ENTREGUE',     'SMS',      'FALHA',   'Provedor SMS indisponivel (fallback tambem falhou)'),
    (12, 12, NULL, NULL, 9, 'PAGAMENTO_RECEBIDO',  'EMAIL',    'FALHA',   'SMTP timeout apos 3 tentativas'),
    (13, NULL, NULL, NULL, 15, 'ANIVERSARIO_CLIENTE','EMAIL',   'ENVIADA', NULL),
    (14, NULL, NULL, NULL, 3,  'ANIVERSARIO_CLIENTE','EMAIL',   'FALHA',   'Caixa de entrada cheia'),
    (15, NULL, NULL, 11, 4, 'ORCAMENTO_EXPIRANDO', 'EMAIL',    'ENVIADA', NULL),
    (16, NULL, NULL,  4, 5, 'ORCAMENTO_APROVADO',  'WHATSAPP', 'ENVIADA', NULL),
    (17, NULL, NULL,  7, 7, 'ORCAMENTO_RECUSADO',  'EMAIL',    'ENVIADA', NULL),
    (18, NULL, NULL,  3, 4, 'ORCAMENTO_EXPIRANDO', 'WHATSAPP', 'FALHA',   'Cliente sem destinatario valido'),
    (19,  5, NULL, NULL, 5, 'PEDIDO_CONFIRMADO',   'EMAIL',    'FALHA',   'Cliente sem email cadastrado'),
    (20,  7, NULL, NULL, 6, 'PRODUCAO_INICIADA',   'SMS',      'ENVIADA', NULL)
) AS v(n, pedn, x1, orcn, cli, evento, canal, status, erro)
LEFT JOIN cen_ped p ON p.n = v.pedn
LEFT JOIN cen_orc o ON o.n = v.orcn
LEFT JOIN cen_cli c ON c.n = v.cli
WHERE NOT EXISTS (SELECT 1 FROM notificacoes_enviadas x
                  WHERE x.destinatario = CASE WHEN v.canal = 'EMAIL' THEN 'cen.notif.' || lpad(v.n::text, 2, '0') || '@teste.local'
                                              ELSE '(11) 95555-' || lpad(v.n::text, 4, '0') END);

INSERT INTO alertas_internos (mensagem, link, icone, cor, lido, criado_em)
SELECT '[CEN-ALE-' || lpad(v.n::text, 2, '0') || '] ' || v.msg, v.link, v.icone, v.cor, v.lido, now() - v.n * interval '5 hours'
FROM (VALUES
    ( 1, 'Estoque critico de Insumo Critico',                 '/estoque/movimentacoes', 'bi-box-seam',          'warning',   false),
    ( 2, 'Estoque zerado de Insumo Zerado',                   '/estoque/movimentacoes', 'bi-box-seam',          'danger',    false),
    ( 3, 'Ordem em andamento cancelada',                      '/producao',              'bi-clipboard2-x',      'danger',    false),
    ( 4, 'Pedido cancelado com valor ja recebido: devolver',  '/pedidos',               'bi-cash-coin',         'warning',   false),
    ( 5, 'Inspecao de qualidade reprovada',                   '/qualidade',             'bi-shield-exclamation','info',      true),
    ( 6, 'Orcamento aprovado pelo cliente',                   '/orcamentos',            'bi-bell',              'secondary', true),
    ( 7, 'Orcamento recusado pelo cliente',                   '/orcamentos',            'bi-bell',              'secondary', true),
    ( 8, 'Aniversariante do dia',                             '/crm',                   'bi-gift',              'success',   false),
    ( 9, 'Conta a pagar vencida',                             '/financeiro/contas-pagar','bi-cash-coin',        'danger',    false),
    (10, 'Alerta antigo ja lido',                             NULL,                     'bi-bell',              'secondary', true),
    (11, 'Alerta sem link',                                   NULL,                     'bi-bell',              'info',      false),
    (12, 'Alerta com mensagem longa: ' || repeat('texto ', 60), '/alertas',             'bi-exclamation-triangle','warning', false)
) AS v(n, msg, link, icone, cor, lido)
WHERE NOT EXISTS (SELECT 1 FROM alertas_internos a WHERE a.mensagem LIKE '[CEN-ALE-' || lpad(v.n::text, 2, '0') || ']%');

-- ---------------------------------------------------------------------------
-- 15. Auditoria (CEN-AUD)
-- ---------------------------------------------------------------------------
INSERT INTO audit_log (usuario, acao, entidade_tipo, entidade_id, detalhes, criado_em)
SELECT v.usuario, v.acao, v.tipo, v.eid, '[CEN-AUD-' || lpad(v.n::text, 2, '0') || '] ' || v.det, now() - v.n * interval '7 hours'
FROM (VALUES
    ( 1, 'cen_atendente',   'CONFIRMAR_PEDIDO',   'Pedido',    4,  'Pedido confirmado'),
    ( 2, 'cen_atendente',   'CANCELAR_PEDIDO',    'Pedido',    17, 'Pedido cancelado com pagamento'),
    ( 3, 'cen_confeiteiro', 'PEDIDO_PRONTO',      'Pedido',    8,  'Pedido pronto'),
    ( 4, 'cen_atendente',   'ENTREGAR_PEDIDO',    'Pedido',    10, 'Pedido entregue'),
    ( 5, 'cen_admin',       'EXCLUIR_ORCAMENTO',  'Orcamento', 9,  'Orcamento excluido'),
    ( 6, 'cen_atendente',   'CONVERTER_ORCAMENTO','Orcamento', 6,  'Orcamento convertido em pedido 25'),
    ( 7, 'cen_admin',       'SALVAR_USUARIO',     'Usuario',   1,  'Usuario criado'),
    ( 8, 'cen_admin',       'TOGGLE_USUARIO',     'Usuario',   10, 'Usuario inativado'),
    ( 9, 'sistema',         'PEDIDO_PRONTO',      'Pedido',    9,  'Acao executada por job (usuario sistema)'),
    (10, 'sistema',         'ENTREGAR_PEDIDO',    'Pedido',    11, 'Acao executada por job (usuario sistema)'),
    (11, 'cen_financeiro',  'CONFIRMAR_PEDIDO',   'Pedido',    12, 'Outro usuario, mesma acao'),
    (12, 'cen_admin',       'CANCELAR_PEDIDO',    'Pedido',    16, 'Pedido cancelado sem pagamento')
) AS v(n, usuario, acao, tipo, eid, det)
WHERE NOT EXISTS (SELECT 1 FROM audit_log a WHERE a.detalhes LIKE '[CEN-AUD-' || lpad(v.n::text, 2, '0') || ']%');

COMMIT;

ANALYZE;

-- ---------------------------------------------------------------------------
-- 16. Sanidade: cada fluxo precisa de >= 10 cenarios (coluna ok = true) e invariantes = 0
-- ---------------------------------------------------------------------------
SELECT fluxo, qtd, qtd >= minimo AS ok FROM (
    SELECT 'usuarios'  AS fluxo, COUNT(*) AS qtd, 10 AS minimo FROM usuarios WHERE login LIKE 'cen\_%'
    UNION ALL SELECT 'reset-token',     COUNT(*), 10  FROM password_reset_token WHERE token LIKE '00000ce0-0003-%'
    UNION ALL SELECT 'fornecedores',    COUNT(*), 10  FROM fornecedores WHERE nome LIKE '%[CEN-FOR-%'
    UNION ALL SELECT 'materias-primas', COUNT(*), 10  FROM materias_primas WHERE nome LIKE '%[CEN-MP-%'
    UNION ALL SELECT 'mov-estoque',     COUNT(*), 50  FROM movimentacoes_estoque WHERE motivo LIKE '[CEN-EST]%'
    UNION ALL SELECT 'produtos',        COUNT(*), 10  FROM produtos WHERE nome LIKE '%[CEN-PRD-%'
    UNION ALL SELECT 'fichas-tecnicas', COUNT(*), 10  FROM fichas_tecnicas WHERE observacoes LIKE '[CEN-FT]%'
    UNION ALL SELECT 'clientes',        COUNT(*), 20  FROM clientes WHERE nome LIKE '%[CEN-CLI-%'
    UNION ALL SELECT 'pedidos',         COUNT(*), 25  FROM pedidos WHERE token_acompanhamento LIKE '00000ce0-0001-%'
    UNION ALL SELECT 'pedidos-CONFIRMADO>50', COUNT(*), 51 FROM pedidos WHERE token_acompanhamento LIKE '00000ce0-0001-%' AND status = 'CONFIRMADO'
    UNION ALL SELECT 'pagamentos',      COUNT(*), 10  FROM pagamentos WHERE observacoes LIKE '[CEN-PAG]%'
    UNION ALL SELECT 'orcamentos',      COUNT(*), 14  FROM orcamentos WHERE token_aprovacao LIKE '00000ce0-0002-%'
    UNION ALL SELECT 'orcamentos>50',   COUNT(*), 51  FROM orcamentos WHERE token_aprovacao LIKE '00000ce0-0002-%'
    UNION ALL SELECT 'ordens-producao', COUNT(*), 10  FROM ordens_producao WHERE observacoes LIKE '[CEN-OP%'
    UNION ALL SELECT 'kanban-PENDENTE>50',     COUNT(*), 51 FROM ordens_producao WHERE observacoes LIKE '[CEN-OP%' AND status = 'PENDENTE'
    UNION ALL SELECT 'kanban-EM_ANDAMENTO>50', COUNT(*), 51 FROM ordens_producao WHERE observacoes LIKE '[CEN-OP%' AND status = 'EM_ANDAMENTO'
    UNION ALL SELECT 'kanban-CANCELADA>10',    COUNT(*), 11 FROM ordens_producao WHERE observacoes LIKE '[CEN-OP%' AND status = 'CANCELADA'
    UNION ALL SELECT 'checklist',       COUNT(*), 10  FROM checklist_qualidade c JOIN cen_prd p ON p.id = c.produto_id
    UNION ALL SELECT 'inspecoes>50',    COUNT(*), 51  FROM inspecao_qualidade WHERE observacoes LIKE '[CEN-QUA%'
    UNION ALL SELECT 'inspecoes-reprovadas', COUNT(*), 10 FROM inspecao_qualidade WHERE observacoes LIKE '[CEN-QUA]%' AND NOT aprovado
    UNION ALL SELECT 'contas-pagar',    COUNT(*), 10  FROM contas_pagar WHERE descricao LIKE '[CEN-FIN-%'
    UNION ALL SELECT 'despesas-fixas',  COUNT(*), 8   FROM despesas_fixas WHERE descricao LIKE '[CEN-FIN-DF-%'
    UNION ALL SELECT 'gastos',          COUNT(*), 10  FROM gastos_variaveis WHERE descricao LIKE '[CEN-GAS-%'
    UNION ALL SELECT 'pontos',          COUNT(*), 10  FROM pontos_fidelidade WHERE descricao LIKE '[CEN-PTO-%'
    UNION ALL SELECT 'notas-cliente',   COUNT(*), 10  FROM notas_cliente WHERE texto LIKE '[CEN-NTA-%'
    UNION ALL SELECT 'notificacoes',    COUNT(*), 20  FROM notificacoes_enviadas WHERE destinatario LIKE 'cen.notif.%' OR destinatario LIKE '(11) 95555-%'
    UNION ALL SELECT 'alertas',         COUNT(*), 10  FROM alertas_internos WHERE mensagem LIKE '[CEN-ALE-%'
    UNION ALL SELECT 'auditoria',       COUNT(*), 10  FROM audit_log WHERE detalhes LIKE '[CEN-AUD-%'
) s ORDER BY ok, fluxo;

-- Invariantes: todas devem retornar 0
SELECT 'pagamento_acima_do_total' AS invariante, COUNT(*) AS violacoes FROM (
    SELECT p.id FROM pedidos p JOIN pagamentos g ON g.pedido_id = p.id
    WHERE p.token_acompanhamento LIKE '00000ce0-0001-%' GROUP BY p.id, p.total_pedido HAVING SUM(g.valor) > p.total_pedido
) x
UNION ALL SELECT 'saldo_estoque_negativo', COUNT(*) FROM movimentacoes_estoque WHERE motivo LIKE '[CEN-EST]%' AND saldo_apos < 0
UNION ALL SELECT 'token_tamanho_invalido', COUNT(*) FROM pedidos WHERE token_acompanhamento LIKE '00000ce0-0001-%' AND length(token_acompanhamento) <> 36
UNION ALL SELECT 'notificacao_duplicada_enviada', COUNT(*) FROM (
    SELECT pedido_id, evento, canal FROM notificacoes_enviadas WHERE status = 'ENVIADA' AND pedido_id IS NOT NULL
    GROUP BY pedido_id, evento, canal HAVING COUNT(*) > 1) d;

DROP TABLE IF EXISTS cen_for, cen_mp, cen_prd, cen_cli, cen_ped, cen_orc;
