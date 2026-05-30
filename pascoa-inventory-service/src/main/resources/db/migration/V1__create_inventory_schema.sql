-- Inventory Service — Schema V1
-- Banco: pascoa_inventory

CREATE TABLE materias_primas (
    id                  BIGSERIAL       PRIMARY KEY,
    nome                VARCHAR(150)    NOT NULL,
    unidade             VARCHAR(20)     NOT NULL,
    quantidade_estoque  NUMERIC(12,3)   NOT NULL DEFAULT 0,
    estoque_minimo      NUMERIC(12,3)   NOT NULL DEFAULT 0,
    fornecedor_id       BIGINT,
    ativo               BOOLEAN         NOT NULL DEFAULT TRUE,
    criado_em           TIMESTAMP       NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE TABLE movimentacoes_estoque (
    id                BIGSERIAL       PRIMARY KEY,
    materia_prima_id  BIGINT          NOT NULL REFERENCES materias_primas(id),
    tipo              VARCHAR(10)     NOT NULL CHECK (tipo IN ('ENTRADA','SAIDA')),
    quantidade        NUMERIC(12,3)   NOT NULL CHECK (quantidade > 0),
    observacao        VARCHAR(300),
    data_movimentacao TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_mp_ativo           ON materias_primas(ativo);
CREATE INDEX idx_mp_estoque_critico ON materias_primas(quantidade_estoque, estoque_minimo) WHERE ativo = true;
CREATE INDEX idx_mov_materia_prima  ON movimentacoes_estoque(materia_prima_id, data_movimentacao DESC);
