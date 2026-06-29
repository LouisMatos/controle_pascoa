-- FoodFlow v6 Etapa 15 — Chaves de API para integração externa (plano ENTERPRISE).
-- Armazenamos APENAS o SHA-256 da chave; o plain-text é exibido uma única vez
-- na criação e nunca persiste.

CREATE TABLE IF NOT EXISTS platform.api_key (
    id           BIGSERIAL    PRIMARY KEY,
    tenant_id    VARCHAR(50)  NOT NULL REFERENCES platform.tenant(id) ON DELETE CASCADE,
    nome         VARCHAR(100) NOT NULL,
    chave_hash   CHAR(64)     NOT NULL,
    chave_prefix VARCHAR(12)  NOT NULL,                -- ex: "ff_live_a1b2…" para identificação visual
    escopos      VARCHAR(500),                          -- CSV: read,write,admin (default: read)
    ativa        BOOLEAN      NOT NULL DEFAULT TRUE,
    criada_em    TIMESTAMP    NOT NULL DEFAULT NOW(),
    ultima_usada_em TIMESTAMP,
    expira_em    DATE,                                  -- NULL = sem expiração
    CONSTRAINT uq_api_key_hash UNIQUE (chave_hash)
);

CREATE INDEX IF NOT EXISTS idx_api_key_tenant ON platform.api_key (tenant_id) WHERE ativa = TRUE;
CREATE INDEX IF NOT EXISTS idx_api_key_prefix ON platform.api_key (chave_prefix);
