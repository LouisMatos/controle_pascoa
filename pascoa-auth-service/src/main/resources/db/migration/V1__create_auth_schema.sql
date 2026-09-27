-- Auth Service — Schema V1
-- Banco: pascoa_auth

CREATE TABLE usuarios (
    id                 BIGSERIAL     PRIMARY KEY,
    login              VARCHAR(100)  NOT NULL UNIQUE,
    password_hash      TEXT          NOT NULL,
    ativo              BOOLEAN       NOT NULL DEFAULT TRUE,
    totp_ativado       BOOLEAN       NOT NULL DEFAULT FALSE,
    totp_secret        TEXT,
    tentativas_falhas  INT           NOT NULL DEFAULT 0,
    criado_em          TIMESTAMP     NOT NULL DEFAULT NOW(),
    atualizado_em      TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE TABLE usuario_roles (
    usuario_id  BIGINT      NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    role        VARCHAR(30) NOT NULL,
    PRIMARY KEY (usuario_id, role)
);

CREATE INDEX idx_usuarios_login ON usuarios(login);

-- Admin padrão: login=admin, senha=admin123
-- Hash BCrypt gerado com cost 12
INSERT INTO usuarios (login, password_hash, ativo)
VALUES ('admin', '$2a$12$q1L3aw2OHQCnVKI/sAFmreU7UBgPCqG0wHjUbNb7LVLB7wHFj8j5i', TRUE);

INSERT INTO usuario_roles (usuario_id, role)
VALUES (1, 'ADMIN');
