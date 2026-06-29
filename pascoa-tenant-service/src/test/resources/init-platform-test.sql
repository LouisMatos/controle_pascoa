-- Init script do container de teste — equivale ao infra/postgres-platform/init-platform.sql
-- (apenas o schema 'platform' é necessário; Liquibase per-tenant cria os schemas dos tenants
-- via TenantProvisioningService durante os testes).

CREATE SCHEMA IF NOT EXISTS platform AUTHORIZATION postgres;
GRANT ALL PRIVILEGES ON SCHEMA platform TO postgres;
