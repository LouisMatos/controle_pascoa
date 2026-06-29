-- FoodFlow v6 Etapa 6 — Feature Flags
-- Catálogo global + override por tenant. Sempre em schema 'platform'.

CREATE TABLE IF NOT EXISTS platform.feature_flag (
    chave        VARCHAR(80)  PRIMARY KEY,
    descricao    VARCHAR(500) NOT NULL,
    -- Default por plano: indica se a flag fica ON automaticamente em cada plano
    default_trial      BOOLEAN NOT NULL DEFAULT FALSE,
    default_starter    BOOLEAN NOT NULL DEFAULT FALSE,
    default_pro        BOOLEAN NOT NULL DEFAULT FALSE,
    default_enterprise BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS platform.feature_flag_tenant (
    tenant_id  VARCHAR(50) NOT NULL REFERENCES platform.tenant(id) ON DELETE CASCADE,
    chave      VARCHAR(80) NOT NULL REFERENCES platform.feature_flag(chave) ON DELETE CASCADE,
    ativo      BOOLEAN     NOT NULL,
    atualizado_em TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (tenant_id, chave)
);

CREATE INDEX IF NOT EXISTS ix_ffl_tenant ON platform.feature_flag_tenant (tenant_id);

-- Seed de flags conhecidas pelo sistema (alinhadas ao design v6 §3.2)
INSERT INTO platform.feature_flag (chave, descricao, default_trial, default_starter, default_pro, default_enterprise) VALUES
    ('NOTIFICACAO_WHATSAPP',  'Envio de notificações via WhatsApp',         FALSE, FALSE, TRUE,  TRUE),
    ('NOTIFICACAO_SMS',       'Fallback de notificações via SMS',            FALSE, FALSE, TRUE,  TRUE),
    ('CATALOGO_PUBLICO',      'Catálogo público com QR Code',                TRUE,  TRUE,  TRUE,  TRUE),
    ('ORCAMENTOS_PUBLICOS',   'Aprovação pública de orçamento por token',    FALSE, FALSE, TRUE,  TRUE),
    ('API_PUBLICA',           'API REST pública para integrações (OpenAPI)', FALSE, FALSE, FALSE, TRUE),
    ('DOMINIO_PROPRIO',       'White-label com domínio próprio',             FALSE, FALSE, FALSE, TRUE),
    ('ANALYTICS_AVANCADO',    'Comparativo de safras + ranking',             FALSE, FALSE, TRUE,  TRUE),
    ('EXPORTACAO_PDF_EXCEL',  'Exportação de relatórios em PDF/Excel',       FALSE, FALSE, TRUE,  TRUE)
ON CONFLICT (chave) DO NOTHING;
