package br.com.seuprojeto.pascoa.commons.featureflag;

import br.com.seuprojeto.pascoa.commons.tenant.TenantContext;

/**
 * Helper estático que liga {@link TenantContext} ao {@link FeatureFlagClient}.
 *
 * <p>Uso típico nos serviços:
 * <pre>{@code
 * if (FeatureFlags.isAtivo("NOTIFICACAO_WHATSAPP")) {
 *     enviarViaWhatsApp(...);
 * } else {
 *     enviarPorEmail(...);
 * }
 * }</pre>
 *
 * <p>Cada serviço deve registrar UM bean {@link FeatureFlagClient}
 * (HTTP, cache, mock) — {@link #setClient(FeatureFlagClient)} é chamado por
 * autoconfig na inicialização.
 */
public final class FeatureFlags {

    private static volatile FeatureFlagClient client = new NoOpFeatureFlagClient();

    private FeatureFlags() {}

    public static void setClient(FeatureFlagClient client) {
        FeatureFlags.client = (client != null) ? client : new NoOpFeatureFlagClient();
    }

    public static boolean isAtivo(String chave) {
        return client.isAtivo(TenantContext.get(), chave);
    }

    public static boolean isAtivo(String tenantId, String chave) {
        return client.isAtivo(tenantId, chave);
    }
}
