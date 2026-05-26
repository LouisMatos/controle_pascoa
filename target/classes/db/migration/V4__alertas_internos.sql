-- V4: tabela de notificações internas (sino de alertas para a equipe)
CREATE TABLE IF NOT EXISTS alertas_internos (
    id         BIGSERIAL PRIMARY KEY,
    mensagem   VARCHAR(500)  NOT NULL,
    link       VARCHAR(300),
    icone      VARCHAR(60)   NOT NULL DEFAULT 'bi-bell',
    cor        VARCHAR(20)   NOT NULL DEFAULT 'secondary',
    lido       BOOLEAN       NOT NULL DEFAULT FALSE,
    criado_em  TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_alerta_lido ON alertas_internos (lido, criado_em DESC);
