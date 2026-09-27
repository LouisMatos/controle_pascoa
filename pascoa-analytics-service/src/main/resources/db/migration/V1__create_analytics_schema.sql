-- Analytics Service — Schema V1
-- Banco: pascoa_analytics

CREATE TABLE registros_venda (
    id            BIGSERIAL       PRIMARY KEY,
    pedido_id     BIGINT          NOT NULL,
    cliente_id    BIGINT          NOT NULL,
    produto_id    BIGINT          NOT NULL,
    nome_produto  VARCHAR(150)    NOT NULL,
    quantidade    INT             NOT NULL DEFAULT 1,
    valor_total   NUMERIC(12,2)   NOT NULL DEFAULT 0,
    data_venda    DATE            NOT NULL,
    ano           INT             NOT NULL,
    mes           INT             NOT NULL,
    criado_em     TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- Índices para as queries de analytics
CREATE INDEX idx_rv_ano          ON registros_venda(ano);
CREATE INDEX idx_rv_ano_mes      ON registros_venda(ano, mes);
CREATE INDEX idx_rv_produto_ano  ON registros_venda(produto_id, ano);
CREATE INDEX idx_rv_cliente_ano  ON registros_venda(cliente_id, ano);
CREATE INDEX idx_rv_pedido       ON registros_venda(pedido_id);
