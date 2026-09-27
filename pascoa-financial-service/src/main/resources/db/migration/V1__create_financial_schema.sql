-- Financial Service — Schema V1
-- Banco: pascoa_financial

CREATE TABLE lancamentos (
    id            BIGSERIAL       PRIMARY KEY,
    tipo          VARCHAR(10)     NOT NULL CHECK (tipo IN ('RECEITA','DESPESA')),
    categoria     VARCHAR(20)     NOT NULL,
    descricao     VARCHAR(300)    NOT NULL,
    valor         NUMERIC(12,2)   NOT NULL CHECK (valor >= 0),
    data          DATE            NOT NULL,
    referencia_id VARCHAR(50),
    origem        VARCHAR(50),
    criado_em     TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_lanc_data    ON lancamentos(data);
CREATE INDEX idx_lanc_mes_ano ON lancamentos(EXTRACT(YEAR FROM data), EXTRACT(MONTH FROM data));
CREATE INDEX idx_lanc_tipo    ON lancamentos(tipo);
CREATE INDEX idx_lanc_ref     ON lancamentos(referencia_id, origem);
