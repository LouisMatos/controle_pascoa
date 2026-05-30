-- Production Service — Schema V1
-- Banco: pascoa_production

CREATE TABLE ordens_producao (
    id             BIGSERIAL     PRIMARY KEY,
    pedido_id      BIGINT        NOT NULL UNIQUE,
    nome_cliente   VARCHAR(150)  NOT NULL,
    status         VARCHAR(20)   NOT NULL DEFAULT 'PENDENTE',
    data_previsao  DATE,
    observacoes    VARCHAR(500),
    criado_em      TIMESTAMP     NOT NULL DEFAULT NOW(),
    atualizado_em  TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE TABLE itens_ordem (
    id            BIGSERIAL     PRIMARY KEY,
    ordem_id      BIGINT        NOT NULL REFERENCES ordens_producao(id) ON DELETE CASCADE,
    produto_id    BIGINT        NOT NULL,
    nome_produto  VARCHAR(150)  NOT NULL,
    quantidade    INT           NOT NULL CHECK (quantidade > 0)
);

CREATE INDEX idx_ordens_status    ON ordens_producao(status);
CREATE INDEX idx_ordens_pedido    ON ordens_producao(pedido_id);
CREATE INDEX idx_ordens_previsao  ON ordens_producao(data_previsao) WHERE status NOT IN ('CONCLUIDA','CANCELADA');
CREATE INDEX idx_itens_ordem_id   ON itens_ordem(ordem_id);
