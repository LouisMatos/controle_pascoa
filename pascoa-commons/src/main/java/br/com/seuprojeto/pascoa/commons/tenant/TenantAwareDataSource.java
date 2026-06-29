package br.com.seuprojeto.pascoa.commons.tenant;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DataSource wrapper que, a cada {@link #getConnection()}, executa
 * {@code SET search_path TO "{tenant}", public} usando o tenantId do {@link TenantContext}.
 * <p>Adequado para PostgreSQL Schema-per-Tenant. Schemas inexistentes devem ser
 * provisionados pelo tenant-service antes de chegar requisição.
 */
public class TenantAwareDataSource extends DelegatingDataSource {

    public TenantAwareDataSource(DataSource delegate) {
        super(delegate);
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection conn = super.getConnection();
        applySearchPath(conn);
        return conn;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection conn = super.getConnection(username, password);
        applySearchPath(conn);
        return conn;
    }

    private void applySearchPath(Connection conn) throws SQLException {
        String tenant = TenantContext.get();
        if (tenant == null || tenant.isBlank()) {
            return;
        }
        // Validação defensiva — só [a-z0-9_-] permitido para evitar SQL injection
        if (!tenant.matches("[a-zA-Z0-9_-]+")) {
            throw new SQLException("tenantId inválido: " + tenant);
        }
        try (Statement st = conn.createStatement()) {
            st.execute("SET search_path TO \"" + tenant + "\", public");
        }
    }
}
