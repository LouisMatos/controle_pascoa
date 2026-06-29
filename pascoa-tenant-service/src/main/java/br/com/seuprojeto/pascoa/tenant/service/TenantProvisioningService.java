package br.com.seuprojeto.pascoa.tenant.service;

import br.com.seuprojeto.pascoa.tenant.exception.TenantException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Provisionamento físico do schema PostgreSQL de um tenant.
 *
 * <p>Fluxo em duas fases:
 * <ol>
 *   <li>{@link #provisionarSchema(String)} — CREATE SCHEMA idempotente.</li>
 *   <li>{@link LiquibaseTenantMigrator#migrar(String)} — aplica o changelog
 *       per-tenant (tabelas {@code _meta} e {@code tenant_config_local}).</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantProvisioningService {

    private final JdbcTemplate jdbcTemplate;
    private final LiquibaseTenantMigrator liquibaseTenantMigrator;

    /** Cria o schema dedicado do tenant + aplica todas as migrations. Idempotente. */
    public void provisionarSchema(String tenantId) {
        validarTenantId(tenantId);
        log.info("Provisionando schema do tenant: {}", tenantId);
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS \"" + tenantId + "\"");
        log.info("Schema '{}' criado/já existia — aplicando migrations Liquibase", tenantId);
        liquibaseTenantMigrator.migrar(tenantId);
    }

    /** Remove o schema do tenant (cancelamento + anonimização LGPD). Idempotente. */
    public void removerSchema(String tenantId) {
        validarTenantId(tenantId);
        log.warn("Removendo schema do tenant: {}", tenantId);
        jdbcTemplate.execute("DROP SCHEMA IF EXISTS \"" + tenantId + "\" CASCADE");
    }

    private void validarTenantId(String tenantId) {
        if (tenantId == null || !tenantId.matches("[a-z0-9-]{3,50}")) {
            throw new TenantException("tenantId inválido para SQL: " + tenantId);
        }
    }
}
