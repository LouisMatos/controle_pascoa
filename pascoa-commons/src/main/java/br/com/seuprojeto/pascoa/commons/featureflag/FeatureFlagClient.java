package br.com.seuprojeto.pascoa.commons.featureflag;

/**
 * Contrato consumido pelos microsserviços para resolver feature flags do tenant atual.
 *
 * <p>Implementações esperadas:
 * <ul>
 *   <li>{@link NoOpFeatureFlagClient} — default; tudo desligado (modo single-tenant v5).</li>
 *   <li>HTTP/Feign client contra {@code GET /tenants/{tenantId}/feature-flags/{chave}}
 *       no pascoa-tenant-service — produção multi-tenant.</li>
 *   <li>Cacheado em Redis com TTL 5min — recomendado para hot paths.</li>
 * </ul>
 *
 * <p>Use via {@link FeatureFlags#isAtivo(String)} para inferir o tenant do {@code TenantContext}.
 */
public interface FeatureFlagClient {
    boolean isAtivo(String tenantId, String chave);
}
