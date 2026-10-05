-- v2 Item 2: Entidades financeiras novas (sem alterar tabelas existentes)
-- O ddl-auto=update cria estas tabelas automaticamente ao subir.
-- Este script serve de referência e para execução manual se necessário.

CREATE TABLE IF NOT EXISTS despesas_fixas (
    id                  BIGSERIAL PRIMARY KEY,
    descricao           VARCHAR(200) NOT NULL,
    valor               NUMERIC(10,2) NOT NULL,
    periodicidade       VARCHAR(10)  NOT NULL,
    ativo               BOOLEAN      NOT NULL DEFAULT TRUE,
    data_inicio         DATE         NOT NULL,
    data_cancelamento   DATE
);

CREATE TABLE IF NOT EXISTS despesas_variaveis (
    id          BIGSERIAL PRIMARY KEY,
    pedido_id   BIGINT       NOT NULL REFERENCES pedidos(id),
    descricao   VARCHAR(200) NOT NULL,
    valor       NUMERIC(10,2) NOT NULL,
    categoria   VARCHAR(20)  NOT NULL
);

CREATE TABLE IF NOT EXISTS configuracao_financeira (
    id                      BIGSERIAL PRIMARY KEY,
    margem_desejada_padrao  NUMERIC(5,2)  NOT NULL DEFAULT 30.00,
    aliquota_simples        NUMERIC(5,2)  NOT NULL DEFAULT 6.00,
    meta_faturamento_mensal NUMERIC(12,2) NOT NULL DEFAULT 0,
    moeda                   VARCHAR(5)    NOT NULL DEFAULT 'BRL'
);

CREATE TABLE IF NOT EXISTS contas_receber (
    id              BIGSERIAL PRIMARY KEY,
    pedido_id       BIGINT        NOT NULL REFERENCES pedidos(id),
    valor_original  NUMERIC(10,2) NOT NULL,
    valor_pago      NUMERIC(10,2) DEFAULT 0,
    vencimento      DATE          NOT NULL,
    status          VARCHAR(10)   NOT NULL DEFAULT 'ABERTA'
);

CREATE TABLE IF NOT EXISTS contas_pagar (
    id              BIGSERIAL PRIMARY KEY,
    fornecedor_id   BIGINT        REFERENCES fornecedores(id),
    descricao       VARCHAR(200)  NOT NULL,
    valor           NUMERIC(10,2) NOT NULL,
    vencimento      DATE          NOT NULL,
    status          VARCHAR(10)   NOT NULL DEFAULT 'ABERTA',
    categoria       VARCHAR(20)   NOT NULL
);

-- Registro inicial de configuração (singleton)
INSERT INTO configuracao_financeira (margem_desejada_padrao, aliquota_simples, meta_faturamento_mensal, moeda)
SELECT 30.00, 6.00, 0, 'BRL'
WHERE NOT EXISTS (SELECT 1 FROM configuracao_financeira);
