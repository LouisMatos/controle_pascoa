package br.com.seuprojeto.pascoa.tenant.service;

import br.com.seuprojeto.pascoa.tenant.exception.TenantException;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Aplica o changelog per-tenant (db/tenant-changelog/db.changelog-master.yaml)
 * dentro do schema dedicado do tenant.
 *
 * <p>Mantém a tabela {@code databasechangelog} dentro do próprio schema do tenant —
 * cada tenant tem seu histórico isolado, o que viabiliza migrations futuras evolutivas.
 */
@Service
@Slf4j
public class LiquibaseTenantMigrator {

    private static final String CHANGELOG = "db/tenant-changelog/db.changelog-master.yaml";

    private final DataSource dataSource;

    public LiquibaseTenantMigrator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Executa todas as migrations pendentes contra o schema {@code tenantId}.
     * Idempotente — apenas changesets ainda não aplicados são executados.
     */
    public void migrar(String tenantId) {
        if (tenantId == null || !tenantId.matches("[a-z0-9-]{3,50}")) {
            throw new TenantException("tenantId inválido para Liquibase: " + tenantId);
        }
        log.info("Aplicando changelog '{}' no schema '{}'", CHANGELOG, tenantId);
        try (Connection conn = dataSource.getConnection()) {
            JdbcConnection jdbcConn = new JdbcConnection(conn);
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(jdbcConn);
            // Direciona TODOS os objetos (DDL + tabelas internas de tracking) ao schema do tenant
            database.setDefaultSchemaName(tenantId);
            database.setLiquibaseSchemaName(tenantId);

            try (Liquibase liquibase = new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(), database)) {
                liquibase.update("");
            }
            log.info("Migrations aplicadas com sucesso no schema '{}'", tenantId);
        } catch (Exception e) {
            throw new TenantException("Falha ao aplicar Liquibase no schema " + tenantId, e);
        }
    }
}
