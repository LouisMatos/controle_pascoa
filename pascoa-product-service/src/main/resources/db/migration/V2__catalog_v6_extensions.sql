-- FoodFlow v6 Etapa 8 — catalog/product-service
-- Estende o catálogo com: tenant_id, categoria customizada, atributos dinâmicos,
-- variantes de produto e ficha nutricional. Mantém compatibilidade com schema v5.

-- 1) Multi-tenant
ALTER TABLE produtos ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(50);
ALTER TABLE produtos ADD COLUMN IF NOT EXISTS categoria_customizada VARCHAR(80);
ALTER TABLE produtos ADD COLUMN IF NOT EXISTS sazonal BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE produtos ADD COLUMN IF NOT EXISTS sazonal_inicio DATE;
ALTER TABLE produtos ADD COLUMN IF NOT EXISTS sazonal_fim    DATE;

CREATE INDEX IF NOT EXISTS idx_produtos_tenant ON produtos(tenant_id) WHERE ativo = true;
CREATE INDEX IF NOT EXISTS idx_produtos_categoria_custom ON produtos(categoria_customizada) WHERE ativo = true;
CREATE INDEX IF NOT EXISTS idx_produtos_sazonal ON produtos(sazonal_inicio, sazonal_fim) WHERE sazonal = true;

-- 2) Atributos dinâmicos (configurados pelo tenant via config-engine)
-- Modelo key/value — schema do tenant decide quais chaves são válidas.
CREATE TABLE IF NOT EXISTS produto_atributo (
    id              BIGSERIAL PRIMARY KEY,
    produto_id      BIGINT       NOT NULL REFERENCES produtos(id) ON DELETE CASCADE,
    atributo_key    VARCHAR(80)  NOT NULL,
    atributo_value  VARCHAR(500),
    CONSTRAINT uq_produto_atributo UNIQUE (produto_id, atributo_key)
);
CREATE INDEX IF NOT EXISTS idx_atributo_produto ON produto_atributo(produto_id);
CREATE INDEX IF NOT EXISTS idx_atributo_key     ON produto_atributo(atributo_key);

-- 3) Variantes — mesmo produto com SKU/preço/atributos diferentes
-- Ex: "Bolo Brigadeiro" com variantes 15cm/20cm/25cm
CREATE TABLE IF NOT EXISTS produto_variante (
    id             BIGSERIAL PRIMARY KEY,
    produto_id     BIGINT          NOT NULL REFERENCES produtos(id) ON DELETE CASCADE,
    sku            VARCHAR(50),
    nome           VARCHAR(150)    NOT NULL,
    preco          NUMERIC(10,2)   NOT NULL CHECK (preco > 0),
    atributos_json TEXT,
    disponivel     BOOLEAN         NOT NULL DEFAULT TRUE,
    criado_em      TIMESTAMP       NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_produto_sku UNIQUE (sku)
);
CREATE INDEX IF NOT EXISTS idx_variante_produto ON produto_variante(produto_id) WHERE disponivel = true;

-- 4) Ficha nutricional — útil para marmitaria, doces, restaurante
CREATE TABLE IF NOT EXISTS produto_ficha_nutricional (
    produto_id        BIGINT PRIMARY KEY REFERENCES produtos(id) ON DELETE CASCADE,
    porcao_g          NUMERIC(8,2),
    calorias          NUMERIC(8,2),
    proteinas_g       NUMERIC(8,2),
    carboidratos_g    NUMERIC(8,2),
    gorduras_g        NUMERIC(8,2),
    gorduras_saturadas_g NUMERIC(8,2),
    fibras_g          NUMERIC(8,2),
    sodio_mg          NUMERIC(8,2),
    acucares_g        NUMERIC(8,2),
    sem_gluten        BOOLEAN NOT NULL DEFAULT FALSE,
    sem_lactose       BOOLEAN NOT NULL DEFAULT FALSE,
    vegano            BOOLEAN NOT NULL DEFAULT FALSE,
    vegetariano       BOOLEAN NOT NULL DEFAULT FALSE,
    alergenos         VARCHAR(500),
    atualizado_em     TIMESTAMP NOT NULL DEFAULT NOW()
);
