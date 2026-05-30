-- 2FA TOTP para usuários ADMIN
ALTER TABLE usuarios
    ADD COLUMN IF NOT EXISTS totp_secret           VARCHAR(64),
    ADD COLUMN IF NOT EXISTS totp_ativado          BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS tentativas_totp_falhas INTEGER NOT NULL DEFAULT 0;
