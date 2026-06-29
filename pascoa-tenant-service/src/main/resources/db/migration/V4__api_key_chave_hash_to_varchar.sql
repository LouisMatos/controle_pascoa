-- FoodFlow v6 — fix: alinhar tipo de `chave_hash` ao mapeamento JPA da entidade ApiKey.
-- V3 criou a coluna como CHAR(64), mas a entidade declara @Column(length=64) → VARCHAR(64).
-- Hibernate (ddl-auto=validate) abortava o boot com SchemaManagementException, derrubando
-- toda a cadeia v6 do start-all.sh (set -e).

ALTER TABLE platform.api_key
    ALTER COLUMN chave_hash TYPE VARCHAR(64);
