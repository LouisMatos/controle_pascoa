-- FoodFlow v6 Etapa 11 — subscription-service
-- Schema 'subscription' já criado pelo init-platform.sql.

CREATE TABLE IF NOT EXISTS subscription.assinatura (
    tenant_id              VARCHAR(50)  PRIMARY KEY,
    plano                  VARCHAR(20)  NOT NULL,
    status                 VARCHAR(20)  NOT NULL,
    stripe_customer_id     VARCHAR(80),
    stripe_subscription_id VARCHAR(80) UNIQUE,
    stripe_price_id        VARCHAR(80),
    trial_termina_em       DATE,
    periodo_fim            DATE,
    criada_em              TIMESTAMP    NOT NULL DEFAULT NOW(),
    atualizada_em          TIMESTAMP
);

CREATE INDEX IF NOT EXISTS ix_assinatura_status ON subscription.assinatura (status);
CREATE INDEX IF NOT EXISTS ix_assinatura_trial_em ON subscription.assinatura (trial_termina_em)
    WHERE status = 'TRIALING';

CREATE TABLE IF NOT EXISTS subscription.uso_metrico (
    tenant_id          VARCHAR(50)  NOT NULL,
    competencia        VARCHAR(7)   NOT NULL,    -- 'YYYY-MM'
    pedidos_total      INT          NOT NULL DEFAULT 0,
    usuarios_ativos    INT          NOT NULL DEFAULT 0,
    notificacoes_total INT          NOT NULL DEFAULT 0,
    armazenamento_mb   BIGINT       NOT NULL DEFAULT 0,
    atualizado_em      TIMESTAMP    NOT NULL DEFAULT NOW(),
    PRIMARY KEY (tenant_id, competencia)
);

CREATE INDEX IF NOT EXISTS ix_uso_metrico_tenant ON subscription.uso_metrico (tenant_id);
