-- V12: Item 23 — Bugs médios e fluxos
-- B7: campo 'evento' em notificacoes_enviadas + índice de idempotência
-- B9: campo 'desconsiderar_no_custo' em gastos_variaveis + FK pedido_id

-- ── B7: Idempotência de notificações ──────────────────────────────────────────
ALTER TABLE notificacoes_enviadas
    ADD COLUMN IF NOT EXISTS evento VARCHAR(40);

-- Índice parcial: impede duplicata para um mesmo (pedido, evento, canal) enviado com sucesso
CREATE UNIQUE INDEX IF NOT EXISTS uq_notif_pedido_evento_canal
    ON notificacoes_enviadas (pedido_id, evento, canal)
    WHERE status = 'ENVIADA';

-- ── B9 / F6: Gastos desconsiderados (cancelamento de pedido) ─────────────────
ALTER TABLE gastos_variaveis
    ADD COLUMN IF NOT EXISTS desconsiderar_no_custo BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE gastos_variaveis
    ADD COLUMN IF NOT EXISTS pedido_id BIGINT REFERENCES pedidos(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_gasto_pedido ON gastos_variaveis(pedido_id)
    WHERE pedido_id IS NOT NULL;
