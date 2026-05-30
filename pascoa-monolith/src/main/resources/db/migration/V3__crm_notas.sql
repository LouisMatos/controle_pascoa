-- =============================================================
-- V3 — CRM: tabela de notas internas por cliente
-- =============================================================
CREATE TABLE IF NOT EXISTS notas_cliente (
    id          BIGSERIAL PRIMARY KEY,
    cliente_id  BIGINT        NOT NULL REFERENCES clientes(id),
    texto       VARCHAR(2000) NOT NULL,
    criado_em   TIMESTAMP     NOT NULL DEFAULT NOW(),
    criado_por  VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_nota_cliente ON notas_cliente (cliente_id);
