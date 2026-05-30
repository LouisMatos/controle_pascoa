-- Customer Service — Schema V1
-- Banco: pascoa_customers

CREATE TABLE clientes (
    id                BIGSERIAL     PRIMARY KEY,
    nome              VARCHAR(150)  NOT NULL,
    email             VARCHAR(150)  NOT NULL UNIQUE,
    telefone          VARCHAR(20),
    cpf               VARCHAR(14),
    data_nascimento   DATE,
    endereco_entrega  VARCHAR(300),
    preferencia_canal VARCHAR(20)   NOT NULL DEFAULT 'EMAIL',
    pontos_fidelidade INT           NOT NULL DEFAULT 0,
    ativo             BOOLEAN       NOT NULL DEFAULT TRUE,
    criado_em         TIMESTAMP     NOT NULL DEFAULT NOW(),
    atualizado_em     TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_clientes_email  ON clientes(email);
CREATE INDEX idx_clientes_ativo  ON clientes(ativo);
CREATE INDEX idx_clientes_nasc   ON clientes(data_nascimento);
