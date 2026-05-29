-- V13: F8 — persiste segmento calculado na tabela de clientes
-- O job @Scheduled atualiza este campo diariamente às 02h00

ALTER TABLE clientes
    ADD COLUMN IF NOT EXISTS segmento VARCHAR(20) DEFAULT 'NOVO';
