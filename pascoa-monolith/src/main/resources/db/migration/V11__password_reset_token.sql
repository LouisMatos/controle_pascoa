-- Item 22: Recuperação de Senha
-- Adiciona e-mail ao usuário (utilizado para envio do link de reset)
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS email VARCHAR(150);

-- Tabela de tokens de redefinição de senha
-- Um token é UUID, expira em 30 min e é marcado como usado após utilização.
CREATE TABLE IF NOT EXISTS password_reset_token (
    id          BIGSERIAL PRIMARY KEY,
    usuario_id  BIGINT        NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    token       VARCHAR(36)   NOT NULL UNIQUE,
    expira_em   TIMESTAMP     NOT NULL,
    usado       BOOLEAN       NOT NULL DEFAULT FALSE,
    criado_em   TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_prt_token      ON password_reset_token(token);
CREATE INDEX IF NOT EXISTS idx_prt_usuario    ON password_reset_token(usuario_id);
