-- v2 Item 1: Campos novos nas entidades existentes
-- Executar UMA VEZ no PostgreSQL (pascoa_db)

-- clientes: canal de notificação e opt-in
ALTER TABLE clientes
    ADD COLUMN IF NOT EXISTS preferencia_canal VARCHAR(10) DEFAULT 'NENHUM',
    ADD COLUMN IF NOT EXISTS opt_in            BOOLEAN     DEFAULT FALSE;

-- produtos: margem desejada, foto e período de safra
ALTER TABLE produtos
    ADD COLUMN IF NOT EXISTS margem_desejada NUMERIC(5,2),
    ADD COLUMN IF NOT EXISTS foto            VARCHAR(500),
    ADD COLUMN IF NOT EXISTS inicio_safra    DATE,
    ADD COLUMN IF NOT EXISTS fim_safra       DATE;

-- materias_primas: custo médio ponderado, data da última compra e fornecedor preferencial
ALTER TABLE materias_primas
    ADD COLUMN IF NOT EXISTS custo_medio_ponderado  NUMERIC(10,4) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS data_ultima_compra     DATE,
    ADD COLUMN IF NOT EXISTS fornecedor_preferencial_id BIGINT
        REFERENCES fornecedores(id) ON DELETE SET NULL;

-- pedidos: token de acompanhamento, slot de entrega e indicadores financeiros calculados
ALTER TABLE pedidos
    ADD COLUMN IF NOT EXISTS token_acompanhamento VARCHAR(36),
    ADD COLUMN IF NOT EXISTS slot_entrega         TIME,
    ADD COLUMN IF NOT EXISTS custo_real_calculado  NUMERIC(10,2),
    ADD COLUMN IF NOT EXISTS margem_real_calculada NUMERIC(5,2);

-- Gerar token para pedidos já existentes (sem token)
UPDATE pedidos
SET token_acompanhamento = gen_random_uuid()::text
WHERE token_acompanhamento IS NULL;
