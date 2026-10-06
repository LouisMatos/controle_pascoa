-- Seed leve para testar o fluxo de novo pedido / wizard / confirmacao / producao / orcamento.
-- Nao apaga nada; idempotente (reexecutar nao duplica). Dados marcados com sufixo "(Teste)".
--
--   docker compose exec -T postgres psql -U postgres -d pascoa_monolith < infra/seed/seed-fluxo-pedido.sql
--
-- Cenarios cobertos:
--   * cliente sem telefone/email; produto inativo; produto SEM ficha tecnica; materia-prima com estoque zero
--   * pedidos em todos os status; pedido com entrega no passado; pagamento parcial
--   * orcamento APROVADO valido e orcamento APROVADO expirado

\set ON_ERROR_STOP on

BEGIN;

INSERT INTO fornecedores (nome, cnpj, telefone, email, criado_em, criado_por)
SELECT 'Cacau Brasil (Teste)', '11.222.333/0001-44', '(11) 98888-0001', 'contato@cacau.local', now(), 'seed'
WHERE NOT EXISTS (SELECT 1 FROM fornecedores WHERE nome = 'Cacau Brasil (Teste)');

INSERT INTO materias_primas (nome, unidade, quantidade_atual, quantidade_minima, custo_unitario,
                             custo_medio_ponderado, data_ultima_compra, fornecedor_preferencial_id, criado_em, criado_por)
SELECT v.nome || ' (Teste)', v.unidade, v.qtd, v.minima, v.custo, v.custo, CURRENT_DATE - 5,
       (SELECT id FROM fornecedores WHERE nome = 'Cacau Brasil (Teste)'), now(), 'seed'
FROM (VALUES
    ('Chocolate ao Leite',     'KG', 30.000, 5.000, 42.0000),
    ('Chocolate Meio Amargo',  'KG', 25.000, 5.000, 48.5000),
    ('Chocolate Branco',       'KG', 20.000, 4.000, 51.0000),
    ('Chocolate Diet',         'KG', 10.000, 3.000, 67.9000),
    ('Chocolate Vegano',       'KG',  8.000, 2.000, 72.4000),
    ('Recheio Brigadeiro',     'KG', 12.000, 3.000, 39.9000),
    ('Recheio Maracuja',       'KG',  9.000, 2.000, 44.2000),
    ('Embalagem Ovo',          'UN', 200.000, 20.000, 2.1000),
    ('Laco Decorativo',        'UN', 300.000, 20.000, 0.9000),
    ('Insumo Esgotado',        'UN',  0.000, 10.000, 5.0000)
) AS v(nome, unidade, qtd, minima, custo)
WHERE NOT EXISTS (SELECT 1 FROM materias_primas m WHERE m.nome = v.nome || ' (Teste)');

INSERT INTO produtos (nome, descricao, categoria_id, preco_venda, ativo, margem_desejada, criado_em, criado_por)
SELECT v.nome || ' (Teste)', 'Produto de teste do fluxo de pedido', (SELECT cp.id FROM categorias_produto cp WHERE cp.loja_id = 1 AND upper(cp.nome) = v.categoria), v.preco, v.ativo, 45.00, now(), 'seed'
FROM (VALUES
    ('Ovo Trufado 350g',        'TRUFADO',     89.90, true),
    ('Ovo Recheado 500g',       'RECHEADO',   119.90, true),
    ('Ovo ao Leite 250g',       'TRADICIONAL', 59.90, true),
    ('Ovo Meio Amargo 350g',    'ESPECIAL',    94.90, true),
    ('Ovo Branco 350g',         'TRADICIONAL', 84.90, true),
    ('Ovo Diet 250g',           'DIET',        74.90, true),
    ('Ovo Vegano 350g',         'VEGANO',      99.90, true),
    ('Ovo Sem Ficha 500g',      'ESPECIAL',   109.90, true),
    ('Ovo Descontinuado 1kg',   'TRADICIONAL', 149.90, false)
) AS v(nome, categoria, preco, ativo)
WHERE NOT EXISTS (SELECT 1 FROM produtos p WHERE p.nome = v.nome || ' (Teste)');

INSERT INTO fichas_tecnicas (produto_id, rendimento, unidade_rendimento, observacoes)
SELECT p.id, 1.000, 'UN', 'Ficha de teste'
FROM produtos p
WHERE p.nome LIKE '% (Teste)'
  AND p.nome NOT IN ('Ovo Sem Ficha 500g (Teste)', 'Ovo Descontinuado 1kg (Teste)')
  AND NOT EXISTS (SELECT 1 FROM fichas_tecnicas f WHERE f.produto_id = p.id);

INSERT INTO fichas_tecnicas_itens (ficha_tecnica_id, materia_prima_id, quantidade)
SELECT f.id, m.id, v.qtd
FROM (VALUES
    ('Ovo Trufado 350g',      'Chocolate ao Leite',    0.250),
    ('Ovo Trufado 350g',      'Embalagem Ovo',         1.000),
    ('Ovo Recheado 500g',     'Chocolate Meio Amargo', 0.300),
    ('Ovo Recheado 500g',     'Recheio Brigadeiro',    0.200),
    ('Ovo Recheado 500g',     'Embalagem Ovo',         1.000),
    ('Ovo ao Leite 250g',     'Chocolate ao Leite',    0.250),
    ('Ovo ao Leite 250g',     'Laco Decorativo',       1.000),
    ('Ovo Meio Amargo 350g',  'Chocolate Meio Amargo', 0.350),
    ('Ovo Branco 350g',       'Chocolate Branco',      0.350),
    ('Ovo Branco 350g',       'Recheio Maracuja',      0.100),
    ('Ovo Diet 250g',         'Chocolate Diet',        0.250),
    ('Ovo Vegano 350g',       'Chocolate Vegano',      0.350),
    ('Ovo Vegano 350g',       'Insumo Esgotado',       1.000)
) AS v(produto, mp, qtd)
JOIN produtos p ON p.nome = v.produto || ' (Teste)'
JOIN fichas_tecnicas f ON f.produto_id = p.id
JOIN materias_primas m ON m.nome = v.mp || ' (Teste)'
ON CONFLICT (ficha_tecnica_id, materia_prima_id) DO NOTHING;

INSERT INTO clientes (nome, telefone, email, endereco, cpf, preferencia_canal, opt_in, data_cadastro,
                      anonimizado, segmento, data_nascimento, criado_por)
SELECT v.nome || ' (Teste)', v.tel, v.email, v.endereco, v.cpf, v.canal, v.opt, now() - make_interval(days => v.dias),
       false, v.segmento, v.nasc, 'seed'
FROM (VALUES
    ('Maria Silva',      '(11) 97000-0001', 'maria@teste.local',   'Rua das Amendoas, 100 - Sao Paulo/SP', '11111111111', 'WHATSAPP', true,  300, 'VIP',     DATE '1985-04-12'),
    ('Joao Souza',       '(11) 97000-0002', 'joao@teste.local',    'Av. Paulista, 1500 - Sao Paulo/SP',    '22222222222', 'EMAIL',    true,  200, 'REGULAR', DATE '1990-09-30'),
    ('Ana Oliveira',     '(11) 97000-0003', 'ana@teste.local',     'Rua Augusta, 25 - Sao Paulo/SP',       '33333333333', 'AMBOS',    true,  150, 'REGULAR', NULL),
    ('Pedro Santos',     '(11) 97000-0004', NULL,                  'Rua Oscar Freire, 300 - Sao Paulo/SP', '44444444444', 'WHATSAPP', true,  100, 'NOVO',    DATE '1978-12-01'),
    ('Luiza Pereira',    NULL,              'luiza@teste.local',   NULL,                                   NULL,          'EMAIL',    true,   90, 'NOVO',    NULL),
    ('Carlos Costa',     NULL,              NULL,                  NULL,                                   NULL,          'NENHUM',   false,  60, 'NOVO',    NULL),
    ('Fernanda Almeida', '(11) 97000-0007', 'fernanda@teste.local','Rua Haddock Lobo, 88 - Sao Paulo/SP',  '77777777777', 'AMBOS',    true,  400, 'VIP',     DATE '1995-06-18'),
    ('Rafael Ribeiro',   '(11) 97000-0008', 'rafael@teste.local',  'Rua da Consolacao, 700 - Sao Paulo/SP','88888888888', 'WHATSAPP', false, 500, 'INATIVO', NULL),
    ('Juliana Gomes',    '(11) 97000-0009', 'juliana@teste.local', 'Rua Bela Cintra, 45 - Sao Paulo/SP',   '99999999999', 'EMAIL',    true,   30, 'NOVO',    DATE '2000-01-25'),
    ('Marcos Martins',   '(11) 97000-0010', 'marcos@teste.local',  'Av. Brasil, 2000 - Sao Paulo/SP',      '10101010101', 'WHATSAPP', true,  250, 'REGULAR', NULL)
) AS v(nome, tel, email, endereco, cpf, canal, opt, dias, segmento, nasc)
WHERE NOT EXISTS (SELECT 1 FROM clientes c WHERE c.nome = v.nome || ' (Teste)');

INSERT INTO pedidos (cliente_id, data_pedido, data_entrega, status, observacoes, total_pedido, token_acompanhamento, slot_entrega)
SELECT c.id, now() - make_interval(days => v.dias_atras), CURRENT_DATE + v.entrega, v.status, v.obs, 0,
       md5('fluxo-pedido-' || v.n), make_time(9 + v.n, 0, 0)
FROM (VALUES
    (1, 'Maria Silva',      'NOVO',        5,  7,  'Pedido novo aguardando confirmacao'),
    (2, 'Joao Souza',       'CONFIRMADO',  3,  5,  NULL),
    (3, 'Ana Oliveira',     'EM_PRODUCAO', 4,  2,  'Entregar com laco vermelho'),
    (4, 'Pedro Santos',     'PRONTO',      6,  1,  NULL),
    (5, 'Fernanda Almeida', 'ENTREGUE',    15, -8, NULL),
    (6, 'Rafael Ribeiro',   'CANCELADO',   12, -3, 'Cliente desistiu'),
    (7, 'Juliana Gomes',    'NOVO',        2, -2,  'Data de entrega no passado')
) AS v(n, cliente, status, dias_atras, entrega, obs)
JOIN clientes c ON c.nome = v.cliente || ' (Teste)'
WHERE NOT EXISTS (SELECT 1 FROM pedidos p WHERE p.token_acompanhamento = md5('fluxo-pedido-' || v.n));

INSERT INTO itens_pedido (pedido_id, produto_id, quantidade, preco_unitario, subtotal, custo_unitario)
SELECT p.id, pr.id, v.qtd, pr.preco_venda, pr.preco_venda * v.qtd,
       CASE WHEN p.status IN ('NOVO', 'CANCELADO') THEN NULL ELSE round(pr.preco_venda * 0.45, 2) END
FROM (VALUES
    (1, 'Ovo Trufado 350g',     2),
    (1, 'Ovo ao Leite 250g',    1),
    (2, 'Ovo Recheado 500g',    1),
    (3, 'Ovo Meio Amargo 350g', 3),
    (3, 'Ovo Branco 350g',      1),
    (4, 'Ovo Diet 250g',        2),
    (5, 'Ovo Vegano 350g',      1),
    (5, 'Ovo Trufado 350g',     1),
    (6, 'Ovo Recheado 500g',    2),
    (7, 'Ovo Sem Ficha 500g',   1)
) AS v(n, produto, qtd)
JOIN pedidos p ON p.token_acompanhamento = md5('fluxo-pedido-' || v.n)
JOIN produtos pr ON pr.nome = v.produto || ' (Teste)'
WHERE NOT EXISTS (SELECT 1 FROM itens_pedido i WHERE i.pedido_id = p.id);

UPDATE pedidos p
SET total_pedido = t.total
FROM (SELECT pedido_id, SUM(subtotal) AS total FROM itens_pedido GROUP BY pedido_id) t
WHERE t.pedido_id = p.id AND p.token_acompanhamento IN (SELECT md5('fluxo-pedido-' || g) FROM generate_series(1, 7) g);

INSERT INTO pagamentos (pedido_id, valor, tipo_pagamento, data_pagamento, observacoes)
SELECT p.id, v.valor, v.tipo, CURRENT_DATE - v.dias, v.obs
FROM (VALUES
    (2, 50.00,  'PIX',      2, 'Sinal 50%'),
    (5, 209.80, 'DINHEIRO', 8, 'Pago na entrega')
) AS v(n, valor, tipo, dias, obs)
JOIN pedidos p ON p.token_acompanhamento = md5('fluxo-pedido-' || v.n)
WHERE NOT EXISTS (SELECT 1 FROM pagamentos g WHERE g.pedido_id = p.id);

INSERT INTO orcamentos (cliente_id, data_criacao, validade, status, total, observacoes, token_aprovacao, criado_em, criado_por)
SELECT c.id, now() - make_interval(days => 3), CURRENT_DATE + v.validade_dias, 'APROVADO', 0, v.obs,
       md5('fluxo-orcamento-' || v.n), now(), 'seed'
FROM (VALUES
    (1, 'Ana Oliveira',  10, 'Orcamento aprovado e valido (converter em pedido)'),
    (2, 'Pedro Santos',  -4, 'Orcamento aprovado porem expirado')
) AS v(n, cliente, validade_dias, obs)
JOIN clientes c ON c.nome = v.cliente || ' (Teste)'
WHERE NOT EXISTS (SELECT 1 FROM orcamentos o WHERE o.token_aprovacao = md5('fluxo-orcamento-' || v.n));

INSERT INTO orcamento_itens (orcamento_id, produto_id, quantidade, preco_unitario, subtotal)
SELECT o.id, pr.id, v.qtd, pr.preco_venda, pr.preco_venda * v.qtd
FROM (VALUES
    (1, 'Ovo Trufado 350g',  2),
    (1, 'Ovo Diet 250g',     1),
    (2, 'Ovo Vegano 350g',   1)
) AS v(n, produto, qtd)
JOIN orcamentos o ON o.token_aprovacao = md5('fluxo-orcamento-' || v.n)
JOIN produtos pr ON pr.nome = v.produto || ' (Teste)'
WHERE NOT EXISTS (SELECT 1 FROM orcamento_itens i WHERE i.orcamento_id = o.id);

UPDATE orcamentos o
SET total = t.total
FROM (SELECT orcamento_id, SUM(subtotal) AS total FROM orcamento_itens GROUP BY orcamento_id) t
WHERE t.orcamento_id = o.id AND o.token_aprovacao IN (md5('fluxo-orcamento-1'), md5('fluxo-orcamento-2'));

COMMIT;
