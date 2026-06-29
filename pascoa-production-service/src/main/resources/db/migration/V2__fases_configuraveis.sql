-- FoodFlow v6 Etapa 9 — production-service
-- Adiciona suporte a fases livres definidas pelo tenant (alinhadas ao config-engine).
-- A enum legada StatusOrdem (PENDENTE/EM_ANDAMENTO/CONCLUIDA/CANCELADA) coexiste como
-- "status macro"; a fase_atual representa onde dentro do fluxo do tenant a ordem está.

ALTER TABLE ordens_producao ADD COLUMN IF NOT EXISTS tenant_id   VARCHAR(50);
ALTER TABLE ordens_producao ADD COLUMN IF NOT EXISTS fase_atual  VARCHAR(80);
ALTER TABLE ordens_producao ADD COLUMN IF NOT EXISTS fase_ordem  INT NOT NULL DEFAULT 0;
ALTER TABLE ordens_producao ADD COLUMN IF NOT EXISTS iniciada_em TIMESTAMP;
ALTER TABLE ordens_producao ADD COLUMN IF NOT EXISTS concluida_em TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_ordens_tenant       ON ordens_producao(tenant_id);
CREATE INDEX IF NOT EXISTS idx_ordens_fase_atual   ON ordens_producao(fase_atual)
    WHERE status NOT IN ('CONCLUIDA', 'CANCELADA');

-- Histórico de transições — permite auditoria do fluxo e cálculo de lead time por fase
CREATE TABLE IF NOT EXISTS historico_fase (
    id          BIGSERIAL PRIMARY KEY,
    ordem_id    BIGINT       NOT NULL REFERENCES ordens_producao(id) ON DELETE CASCADE,
    fase_de     VARCHAR(80),
    fase_para   VARCHAR(80)  NOT NULL,
    ordem_de    INT,
    ordem_para  INT          NOT NULL,
    mudado_em   TIMESTAMP    NOT NULL DEFAULT NOW(),
    mudado_por  VARCHAR(80),
    observacao  VARCHAR(500)
);

CREATE INDEX IF NOT EXISTS idx_historico_ordem ON historico_fase(ordem_id);
CREATE INDEX IF NOT EXISTS idx_historico_em    ON historico_fase(mudado_em);
