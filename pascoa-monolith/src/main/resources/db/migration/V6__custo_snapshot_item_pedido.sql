-- B3: snapshot do custo unitário no momento da confirmação do pedido.
-- Itens já existentes ficam com NULL (custo não era capturado antes).
ALTER TABLE itens_pedido
    ADD COLUMN IF NOT EXISTS custo_unitario NUMERIC(10, 2);
