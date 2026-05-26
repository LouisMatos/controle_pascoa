-- =============================================================
-- V1 BASELINE — Estado do banco antes da adoção do Flyway
-- Este script NÃO é executado em bancos já existentes.
-- Com baseline-on-migrate=true o Flyway registra V1 como
-- "já aplicado" e parte direto para V2+.
-- =============================================================

-- ---------------------------------------------------------------
-- FORNECEDORES
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS fornecedores (
    id            BIGSERIAL PRIMARY KEY,
    nome          VARCHAR(150) NOT NULL,
    cnpj          VARCHAR(18),
    telefone      VARCHAR(20),
    email         VARCHAR(150),
    observacoes   VARCHAR(500),
    criado_em     TIMESTAMP NOT NULL,
    atualizado_em TIMESTAMP,
    criado_por    VARCHAR(100),
    atualizado_por VARCHAR(100)
);

-- ---------------------------------------------------------------
-- CLIENTES
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS clientes (
    id               BIGSERIAL PRIMARY KEY,
    nome             VARCHAR(150) NOT NULL,
    telefone         VARCHAR(20),
    email            VARCHAR(150),
    endereco         VARCHAR(300),
    cpf              VARCHAR(14),
    excluido_em      TIMESTAMP,
    preferencia_canal VARCHAR(10) DEFAULT 'NENHUM',
    opt_in           BOOLEAN      DEFAULT FALSE,
    data_cadastro    TIMESTAMP NOT NULL,
    atualizado_em    TIMESTAMP,
    criado_por       VARCHAR(100),
    atualizado_por   VARCHAR(100)
);

-- ---------------------------------------------------------------
-- PRODUTOS
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS produtos (
    id              BIGSERIAL PRIMARY KEY,
    nome            VARCHAR(150) NOT NULL,
    descricao       VARCHAR(500),
    categoria       VARCHAR(20)  NOT NULL,
    preco_venda     NUMERIC(10,2) NOT NULL,
    ativo           BOOLEAN      NOT NULL DEFAULT TRUE,
    margem_desejada NUMERIC(5,2),
    foto            VARCHAR(500),
    inicio_safra    DATE,
    fim_safra       DATE,
    excluido_em     TIMESTAMP,
    criado_em       TIMESTAMP NOT NULL,
    atualizado_em   TIMESTAMP,
    criado_por      VARCHAR(100),
    atualizado_por  VARCHAR(100)
);

-- ---------------------------------------------------------------
-- MATERIAS_PRIMAS
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS materias_primas (
    id                        BIGSERIAL PRIMARY KEY,
    nome                      VARCHAR(150) NOT NULL,
    unidade                   VARCHAR(5)   NOT NULL,
    quantidade_atual          NUMERIC(12,3) NOT NULL DEFAULT 0,
    quantidade_minima         NUMERIC(12,3) NOT NULL DEFAULT 0,
    custo_unitario            NUMERIC(10,4) NOT NULL DEFAULT 0,
    custo_medio_ponderado     NUMERIC(10,4) DEFAULT 0,
    data_ultima_compra        DATE,
    fornecedor_preferencial_id BIGINT REFERENCES fornecedores(id) ON DELETE SET NULL,
    criado_em                 TIMESTAMP NOT NULL,
    atualizado_em             TIMESTAMP,
    criado_por                VARCHAR(100),
    atualizado_por            VARCHAR(100)
);

-- ---------------------------------------------------------------
-- FICHAS_TECNICAS
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS fichas_tecnicas (
    id                 BIGSERIAL PRIMARY KEY,
    produto_id         BIGINT NOT NULL UNIQUE REFERENCES produtos(id),
    rendimento         NUMERIC(10,3) NOT NULL,
    unidade_rendimento VARCHAR(5)   NOT NULL,
    observacoes        VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS fichas_tecnicas_itens (
    id               BIGSERIAL PRIMARY KEY,
    ficha_tecnica_id BIGINT NOT NULL REFERENCES fichas_tecnicas(id),
    materia_prima_id BIGINT NOT NULL REFERENCES materias_primas(id),
    quantidade       NUMERIC(12,3) NOT NULL,
    UNIQUE (ficha_tecnica_id, materia_prima_id)
);

-- ---------------------------------------------------------------
-- PEDIDOS
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pedidos (
    id                    BIGSERIAL PRIMARY KEY,
    cliente_id            BIGINT        NOT NULL REFERENCES clientes(id),
    data_pedido           TIMESTAMP     NOT NULL,
    data_entrega          DATE,
    status                VARCHAR(20)   NOT NULL DEFAULT 'NOVO',
    observacoes           VARCHAR(500),
    total_pedido          NUMERIC(10,2) NOT NULL DEFAULT 0,
    token_acompanhamento  VARCHAR(36)   UNIQUE,
    slot_entrega          TIME,
    custo_real_calculado  NUMERIC(10,2),
    margem_real_calculada NUMERIC(5,2)
);

CREATE TABLE IF NOT EXISTS itens_pedido (
    id             BIGSERIAL PRIMARY KEY,
    pedido_id      BIGINT        NOT NULL REFERENCES pedidos(id),
    produto_id     BIGINT        NOT NULL REFERENCES produtos(id),
    quantidade     INTEGER       NOT NULL,
    preco_unitario NUMERIC(10,2) NOT NULL,
    subtotal       NUMERIC(12,2)
);

CREATE TABLE IF NOT EXISTS pagamentos (
    id             BIGSERIAL PRIMARY KEY,
    pedido_id      BIGINT        NOT NULL REFERENCES pedidos(id),
    valor          NUMERIC(10,2) NOT NULL,
    tipo_pagamento VARCHAR(20)   NOT NULL,
    data_pagamento DATE          NOT NULL,
    observacoes    VARCHAR(300)
);

-- ---------------------------------------------------------------
-- ESTOQUE
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS movimentacoes_estoque (
    id               BIGSERIAL PRIMARY KEY,
    materia_prima_id BIGINT        NOT NULL REFERENCES materias_primas(id),
    tipo             VARCHAR(10)   NOT NULL,
    quantidade       NUMERIC(12,3) NOT NULL,
    saldo_apos       NUMERIC(12,3) NOT NULL,
    data             TIMESTAMP     NOT NULL,
    motivo           VARCHAR(500),
    usuario          VARCHAR(100),
    custo_unitario   NUMERIC(10,4)
);

-- ---------------------------------------------------------------
-- PRODUCAO
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ordens_producao (
    id              BIGSERIAL PRIMARY KEY,
    pedido_id       BIGINT      REFERENCES pedidos(id),
    produto_id      BIGINT      NOT NULL REFERENCES produtos(id),
    quantidade      INTEGER     NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    data_abertura   TIMESTAMP,
    data_conclusao  TIMESTAMP,
    observacoes     VARCHAR(500)
);

-- ---------------------------------------------------------------
-- SEGURANCA
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS usuarios (
    id    BIGSERIAL PRIMARY KEY,
    nome  VARCHAR(255) NOT NULL,
    login VARCHAR(60)  NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL,
    role  VARCHAR(20)  NOT NULL,
    ativo BOOLEAN      NOT NULL DEFAULT TRUE
);

-- ---------------------------------------------------------------
-- FINANCEIRO (v2)
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS despesas_fixas (
    id                 BIGSERIAL PRIMARY KEY,
    descricao          VARCHAR(200) NOT NULL,
    valor              NUMERIC(10,2) NOT NULL,
    periodicidade      VARCHAR(10)  NOT NULL,
    ativo              BOOLEAN      NOT NULL DEFAULT TRUE,
    data_inicio        DATE         NOT NULL,
    data_cancelamento  DATE
);

CREATE TABLE IF NOT EXISTS despesas_variaveis (
    id        BIGSERIAL PRIMARY KEY,
    pedido_id BIGINT        NOT NULL REFERENCES pedidos(id),
    descricao VARCHAR(200)  NOT NULL,
    valor     NUMERIC(10,2) NOT NULL,
    categoria VARCHAR(20)   NOT NULL
);

CREATE TABLE IF NOT EXISTS configuracao_financeira (
    id                      BIGSERIAL PRIMARY KEY,
    margem_desejada_padrao  NUMERIC(5,2)  NOT NULL DEFAULT 30.00,
    aliquota_simples        NUMERIC(5,2)  NOT NULL DEFAULT 6.00,
    meta_faturamento_mensal NUMERIC(12,2) NOT NULL DEFAULT 0,
    moeda                   VARCHAR(5)    NOT NULL DEFAULT 'BRL'
);

CREATE TABLE IF NOT EXISTS contas_receber (
    id             BIGSERIAL PRIMARY KEY,
    pedido_id      BIGINT        NOT NULL REFERENCES pedidos(id),
    valor_original NUMERIC(10,2) NOT NULL,
    valor_pago     NUMERIC(10,2) DEFAULT 0,
    vencimento     DATE          NOT NULL,
    status         VARCHAR(10)   NOT NULL DEFAULT 'ABERTA'
);

CREATE TABLE IF NOT EXISTS contas_pagar (
    id            BIGSERIAL PRIMARY KEY,
    fornecedor_id BIGINT        REFERENCES fornecedores(id),
    descricao     VARCHAR(200)  NOT NULL,
    valor         NUMERIC(10,2) NOT NULL,
    vencimento    DATE          NOT NULL,
    status        VARCHAR(10)   NOT NULL DEFAULT 'ABERTA',
    categoria     VARCHAR(20)   NOT NULL
);

-- ---------------------------------------------------------------
-- NOTIFICACOES (v2)
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS templates_notificacao (
    id             BIGSERIAL PRIMARY KEY,
    evento_gatilho VARCHAR(30)  NOT NULL,
    canal          VARCHAR(10)  NOT NULL,
    assunto        VARCHAR(200),
    corpo          TEXT         NOT NULL,
    ativo          BOOLEAN      NOT NULL DEFAULT TRUE,
    variaveis      VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS notificacoes_enviadas (
    id            BIGSERIAL PRIMARY KEY,
    pedido_id     BIGINT       NOT NULL REFERENCES pedidos(id),
    template_id   BIGINT       REFERENCES templates_notificacao(id),
    canal         VARCHAR(10)  NOT NULL,
    destinatario  VARCHAR(200) NOT NULL,
    data_envio    TIMESTAMP    NOT NULL DEFAULT NOW(),
    status        VARCHAR(10)  NOT NULL,
    mensagem_erro TEXT
);

CREATE TABLE IF NOT EXISTS configuracao_canal (
    id        BIGSERIAL PRIMARY KEY,
    tipo      VARCHAR(10)  NOT NULL UNIQUE,
    api_url   VARCHAR(300),
    api_key   VARCHAR(300),
    remetente VARCHAR(200),
    ativo     BOOLEAN      NOT NULL DEFAULT FALSE,
    test_mode BOOLEAN      NOT NULL DEFAULT TRUE
);
