-- Product Service — Schema V1
-- Banco: pascoa_products

CREATE TABLE produtos (
    id            BIGSERIAL       PRIMARY KEY,
    nome          VARCHAR(150)    NOT NULL,
    descricao     TEXT,
    preco         NUMERIC(10,2)   NOT NULL CHECK (preco > 0),
    categoria     VARCHAR(30)     NOT NULL,
    foto_url      VARCHAR(500),
    disponivel    BOOLEAN         NOT NULL DEFAULT TRUE,
    ativo         BOOLEAN         NOT NULL DEFAULT TRUE,
    criado_em     TIMESTAMP       NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_produtos_ativo      ON produtos(ativo);
CREATE INDEX idx_produtos_disponivel ON produtos(disponivel) WHERE ativo = true;
CREATE INDEX idx_produtos_categoria  ON produtos(categoria)  WHERE ativo = true;
