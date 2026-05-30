-- Audit Log: registra ações críticas do sistema com usuário responsável
CREATE TABLE IF NOT EXISTS audit_log (
    id            BIGSERIAL    PRIMARY KEY,
    usuario       VARCHAR(100) NOT NULL,
    acao          VARCHAR(100) NOT NULL,
    entidade_tipo VARCHAR(100),
    entidade_id   BIGINT,
    detalhes      VARCHAR(1000),
    criado_em     TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_log_criado_em  ON audit_log (criado_em DESC);
CREATE INDEX IF NOT EXISTS idx_audit_log_usuario     ON audit_log (usuario);
CREATE INDEX IF NOT EXISTS idx_audit_log_entidade    ON audit_log (entidade_tipo, entidade_id);
