-- LGPD: timestamp de consentimento e flag de anonimização
ALTER TABLE clientes
    ADD COLUMN IF NOT EXISTS data_consentimento TIMESTAMP,
    ADD COLUMN IF NOT EXISTS anonimizado        BOOLEAN NOT NULL DEFAULT FALSE;
