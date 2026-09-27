-- Order Service — Schema V1
-- Banco: pascoa_orders

CREATE TABLE pedidos (
    id                  BIGSERIAL       PRIMARY KEY,
    cliente_id          BIGINT          NOT NULL,
    nome_cliente        VARCHAR(150)    NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'NOVO',
    forma_pagamento     VARCHAR(20),
    observacao          VARCHAR(500),
    token_rastreamento  VARCHAR(20)     NOT NULL UNIQUE,
    criado_em           TIMESTAMP       NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE TABLE itens_pedido (
    id              BIGSERIAL       PRIMARY KEY,
    pedido_id       BIGINT          NOT NULL REFERENCES pedidos(id) ON DELETE CASCADE,
    produto_id      BIGINT          NOT NULL,
    nome_produto    VARCHAR(150)    NOT NULL,
    preco_unitario  NUMERIC(10,2)   NOT NULL,
    quantidade      INT             NOT NULL CHECK (quantidade > 0)
);

CREATE INDEX idx_pedidos_status          ON pedidos(status);
CREATE INDEX idx_pedidos_cliente         ON pedidos(cliente_id);
CREATE INDEX idx_pedidos_token           ON pedidos(token_rastreamento);
CREATE INDEX idx_itens_pedido_pedido_id  ON itens_pedido(pedido_id);
