-- FoodFlow v6 Etapa 7 — pricing-engine-service
-- Por ora o serviço é puramente computacional (cálculo de preço a partir do
-- payload). Quando passar a armazenar regras por tenant/produto (futura
-- integração com catalog-service), as tabelas virão neste arquivo.
--
-- Mantemos a migration vazia (no-op) só para registrar o tracker Flyway no
-- schema 'pricing' e desbloquear migrations futuras.
SELECT 1;
