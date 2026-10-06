CREATE TABLE categorias_produto (
    id      BIGSERIAL    PRIMARY KEY,
    loja_id BIGINT       NOT NULL DEFAULT 1 REFERENCES lojas(id),
    nome    VARCHAR(60)  NOT NULL,
    ativo   BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_categorias_produto_loja_nome UNIQUE (loja_id, nome)
);

CREATE INDEX idx_categorias_produto_loja_id ON categorias_produto (loja_id);

INSERT INTO categorias_produto (loja_id, nome)
SELECT l.id, c.nome
FROM lojas l
CROSS JOIN (VALUES ('Trufado'), ('Recheado'), ('Diet'), ('Vegano'), ('Tradicional'), ('Especial')) AS c(nome);

ALTER TABLE produtos ADD COLUMN categoria_id BIGINT REFERENCES categorias_produto(id);

UPDATE produtos p
SET categoria_id = c.id
FROM categorias_produto c
WHERE c.loja_id = p.loja_id AND upper(c.nome) = p.categoria;

CREATE INDEX idx_produtos_categoria_id ON produtos (categoria_id);

ALTER TABLE produtos DROP COLUMN categoria;
