-- =============================================================================
-- FoodFlow v6 — postgres-platform
-- Banco dedicado aos dados de PLATAFORMA (cross-tenant):
--   - registro de tenants            (tenant-service)
--   - configurações por tenant       (config-engine-service)
--   - feature flags                  (config-engine-service)
--   - assinaturas e billing Stripe   (subscription-service)
--
-- IMPORTANTE: dados de DOMÍNIO de cada tenant (pedidos, clientes, etc.) NÃO
-- ficam aqui — vão para schemas dedicados em outros bancos, gerenciados pelo
-- TenantContext + TenantAwareDataSource (pascoa-commons).
--
-- POSTGRES_DB=foodflow_platform já é criado pelo entrypoint do container.
-- Este script executa apenas na PRIMEIRA inicialização.
-- =============================================================================

-- Schemas dedicados aos serviços da plataforma. Mantemos 'public' livre para
-- extensões/objetos compartilhados (ex: extensions Stripe, helpers).
CREATE SCHEMA IF NOT EXISTS platform AUTHORIZATION postgres;  -- tenant-service
CREATE SCHEMA IF NOT EXISTS config   AUTHORIZATION postgres;  -- config-engine-service (Etapa 5)
CREATE SCHEMA IF NOT EXISTS pricing  AUTHORIZATION postgres;  -- pricing-engine-service (Etapa 7)
CREATE SCHEMA IF NOT EXISTS subscription AUTHORIZATION postgres; -- subscription-service (Etapa 11)

COMMENT ON SCHEMA platform IS 'FoodFlow v6 — registro de tenants, white-label, billing, feature flags';
COMMENT ON SCHEMA config   IS 'FoodFlow v6 — config-engine: customizações de tenant (categorias, fases, atributos)';
COMMENT ON SCHEMA pricing  IS 'FoodFlow v6 — pricing-engine: regras de preço por tenant/produto';
COMMENT ON SCHEMA subscription IS 'FoodFlow v6 — subscription-service: planos, assinaturas Stripe, metering';

-- Extensões úteis (UUID + criptografia) — disponíveis no schema public para reuso
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Search path padrão da role postgres neste banco
ALTER ROLE postgres IN DATABASE foodflow_platform SET search_path TO platform, public;

GRANT ALL PRIVILEGES ON SCHEMA platform TO postgres;
GRANT ALL PRIVILEGES ON SCHEMA config   TO postgres;
GRANT ALL PRIVILEGES ON SCHEMA pricing  TO postgres;
GRANT ALL PRIVILEGES ON SCHEMA subscription TO postgres;
GRANT ALL PRIVILEGES ON SCHEMA public   TO postgres;
