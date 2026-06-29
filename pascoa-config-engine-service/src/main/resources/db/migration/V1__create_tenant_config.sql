-- FoodFlow v6 Etapa 5 — config-engine-service
-- Tabela única de customização por tenant. Templates ficam em código (TemplateCatalog).

CREATE TABLE IF NOT EXISTS config.tenant_config (
    tenant_id                VARCHAR(50)  PRIMARY KEY,
    tipo_negocio             VARCHAR(20)  NOT NULL,
    categorias_json          TEXT,
    unidades_json            TEXT,
    fases_producao_json      TEXT,
    atributos_produto_json   TEXT,
    modelo_preco             VARCHAR(20)  NOT NULL,
    antecedencia_min_dias    INT          NOT NULL DEFAULT 0,
    criado_em                TIMESTAMP    NOT NULL DEFAULT NOW(),
    atualizado_em            TIMESTAMP
);

CREATE INDEX IF NOT EXISTS ix_tenant_config_tipo_negocio ON config.tenant_config (tipo_negocio);
