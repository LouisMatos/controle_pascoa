package br.com.seuprojeto.pascoa.commons.whitelabel;

/**
 * Contrato consumido pelos microsserviços para obter o {@link WhiteLabelTheme}
 * vigente do tenant atual ({@link br.com.seuprojeto.pascoa.commons.tenant.TenantContext}).
 *
 * <p>Implementações:
 * <ul>
 *   <li>{@link NoOpWhiteLabelClient} (default) — sempre retorna {@link WhiteLabelTheme#DEFAULT}</li>
 *   <li>HTTP/Feign contra {@code GET /tenants/{tenantId}/white-label} no tenant-service
 *       (com cache Redis 5min recomendado)</li>
 * </ul>
 */
public interface WhiteLabelClient {
    WhiteLabelTheme buscar(String tenantId);
}
