-- =============================================================
-- V2 — Novas tabelas da versão 3 do sistema
-- Criadas antes de implementar os módulos para que
-- ddl-auto=validate passe ao subir a aplicação.
-- =============================================================

-- ---------------------------------------------------------------
-- GASTOS VARIAVEIS
-- Rastreamento de despesas variáveis do negócio por categoria.
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS gastos_variaveis (
    id               BIGSERIAL PRIMARY KEY,
    descricao        VARCHAR(200)  NOT NULL,
    valor            NUMERIC(10,2) NOT NULL,
    data_lancamento  DATE          NOT NULL,
    categoria        VARCHAR(30)   NOT NULL,
    referencia_mes   INTEGER       NOT NULL CHECK (referencia_mes BETWEEN 1 AND 12),
    referencia_ano   INTEGER       NOT NULL,
    observacoes      VARCHAR(500),
    comprovante_url  VARCHAR(500),
    criado_em        TIMESTAMP     NOT NULL DEFAULT NOW(),
    criado_por       VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_gastos_mes_ano
    ON gastos_variaveis (referencia_ano, referencia_mes);

-- ---------------------------------------------------------------
-- ORCAMENTOS GASTO
-- Meta de gastos por categoria e período (orçamento × realizado).
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS orcamentos_gasto (
    id              BIGSERIAL PRIMARY KEY,
    categoria       VARCHAR(30)   NOT NULL,
    valor_orcado    NUMERIC(10,2) NOT NULL,
    referencia_mes  INTEGER       NOT NULL CHECK (referencia_mes BETWEEN 1 AND 12),
    referencia_ano  INTEGER       NOT NULL,
    UNIQUE (categoria, referencia_mes, referencia_ano)
);

-- ---------------------------------------------------------------
-- ORCAMENTOS (cotações para clientes)
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS orcamentos (
    id               BIGSERIAL PRIMARY KEY,
    cliente_id       BIGINT        NOT NULL REFERENCES clientes(id),
    data_criacao     TIMESTAMP     NOT NULL DEFAULT NOW(),
    validade         DATE          NOT NULL,
    status           VARCHAR(15)   NOT NULL DEFAULT 'PENDENTE',
    total            NUMERIC(12,2) NOT NULL DEFAULT 0,
    observacoes      VARCHAR(1000),
    token_aprovacao  VARCHAR(36)   UNIQUE,
    pedido_id        BIGINT        REFERENCES pedidos(id),
    criado_em        TIMESTAMP     NOT NULL DEFAULT NOW(),
    criado_por       VARCHAR(100),
    atualizado_em    TIMESTAMP,
    atualizado_por   VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS orcamento_itens (
    id             BIGSERIAL PRIMARY KEY,
    orcamento_id   BIGINT        NOT NULL REFERENCES orcamentos(id) ON DELETE CASCADE,
    produto_id     BIGINT        NOT NULL REFERENCES produtos(id),
    quantidade     INTEGER       NOT NULL,
    preco_unitario NUMERIC(10,2) NOT NULL,
    subtotal       NUMERIC(12,2) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_orcamento_cliente
    ON orcamentos (cliente_id);
CREATE INDEX IF NOT EXISTS idx_orcamento_status
    ON orcamentos (status);

-- ---------------------------------------------------------------
-- PONTOS FIDELIDADE
-- Histórico de crédito/débito de pontos por cliente.
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pontos_fidelidade (
    id              BIGSERIAL PRIMARY KEY,
    cliente_id      BIGINT       NOT NULL REFERENCES clientes(id),
    pedido_id       BIGINT       REFERENCES pedidos(id),
    pontos          INTEGER      NOT NULL,
    tipo            VARCHAR(10)  NOT NULL,
    descricao       VARCHAR(200),
    data_operacao   TIMESTAMP    NOT NULL DEFAULT NOW(),
    data_expiracao  DATE
);

CREATE INDEX IF NOT EXISTS idx_pontos_cliente
    ON pontos_fidelidade (cliente_id);

-- ---------------------------------------------------------------
-- CHECKLIST QUALIDADE
-- Template de itens de checklist vinculado a um produto.
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS checklist_qualidade (
    id         BIGSERIAL PRIMARY KEY,
    produto_id BIGINT       NOT NULL REFERENCES produtos(id),
    item       VARCHAR(300) NOT NULL,
    ordem      INTEGER      NOT NULL DEFAULT 0,
    ativo      BOOLEAN      NOT NULL DEFAULT TRUE
);

-- ---------------------------------------------------------------
-- INSPECAO QUALIDADE
-- Resultado de inspeção vinculado a uma ordem de produção.
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS inspecao_qualidade (
    id                BIGSERIAL PRIMARY KEY,
    ordem_producao_id BIGINT       NOT NULL REFERENCES ordens_producao(id),
    data_inspecao     TIMESTAMP    NOT NULL DEFAULT NOW(),
    inspetor          VARCHAR(100) NOT NULL,
    aprovado          BOOLEAN      NOT NULL,
    observacoes       VARCHAR(1000),
    itens_verificados JSONB
);

CREATE INDEX IF NOT EXISTS idx_inspecao_ordem
    ON inspecao_qualidade (ordem_producao_id);

-- ---------------------------------------------------------------
-- CAMPANHA REENGAJAMENTO
-- Campanhas segmentadas de re-engajamento de clientes.
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS campanha_reengajamento (
    id                  BIGSERIAL PRIMARY KEY,
    nome                VARCHAR(200) NOT NULL,
    descricao           VARCHAR(1000),
    segmento            VARCHAR(30)  NOT NULL,
    canal               VARCHAR(10)  NOT NULL,
    template_id         BIGINT       REFERENCES templates_notificacao(id),
    data_envio          TIMESTAMP,
    status              VARCHAR(15)  NOT NULL DEFAULT 'RASCUNHO',
    total_destinatarios INTEGER      DEFAULT 0,
    total_enviados      INTEGER      DEFAULT 0,
    criado_em           TIMESTAMP    NOT NULL DEFAULT NOW(),
    criado_por          VARCHAR(100)
);
