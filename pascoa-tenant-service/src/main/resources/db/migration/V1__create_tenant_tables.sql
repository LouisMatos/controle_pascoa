-- FoodFlow v6 — tenant-service / schema platform
-- Tabelas de registro e ciclo de vida dos tenants.

CREATE TABLE IF NOT EXISTS platform.tenant (
    id              VARCHAR(50)  PRIMARY KEY,
    razao_social    VARCHAR(200) NOT NULL,
    cnpj_cpf        VARCHAR(20),
    email           VARCHAR(200) NOT NULL,
    plano           VARCHAR(20)  NOT NULL,
    tipo_negocio    VARCHAR(20)  NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    data_expiracao  DATE,
    dominio_custom  VARCHAR(200),
    criado_em       TIMESTAMP    NOT NULL DEFAULT NOW(),
    atualizado_em   TIMESTAMP,
    CONSTRAINT uq_tenant_email          UNIQUE (email),
    CONSTRAINT uq_tenant_dominio_custom UNIQUE (dominio_custom),
    CONSTRAINT ck_tenant_id_slug        CHECK (id ~ '^[a-z0-9-]{3,50}$')
);

CREATE INDEX IF NOT EXISTS ix_tenant_status      ON platform.tenant (status);
CREATE INDEX IF NOT EXISTS ix_tenant_plano       ON platform.tenant (plano);
CREATE INDEX IF NOT EXISTS ix_tenant_tipo_negocio ON platform.tenant (tipo_negocio);

CREATE TABLE IF NOT EXISTS platform.white_label_config (
    tenant_id      VARCHAR(50) PRIMARY KEY REFERENCES platform.tenant(id) ON DELETE CASCADE,
    nome_app       VARCHAR(100),
    logo_url       VARCHAR(500),
    cor_primaria   VARCHAR(7),
    cor_secundaria VARCHAR(7),
    favicon        VARCHAR(500),
    rodape         VARCHAR(500)
);
