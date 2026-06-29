package br.com.seuprojeto.pascoa.commons.whitelabel;

import br.com.seuprojeto.pascoa.commons.tenant.TenantContext;

/**
 * Helper estático: liga {@link TenantContext} ao {@link WhiteLabelClient}.
 *
 * <pre>{@code
 * WhiteLabelTheme tema = WhiteLabel.atual();
 * pdf.setLogo(tema.logoUrl());
 * pdf.setCabecalho("Pedido — " + tema.nomeApp());
 * }</pre>
 */
public final class WhiteLabel {

    private static volatile WhiteLabelClient client = new NoOpWhiteLabelClient();

    private WhiteLabel() {}

    public static void setClient(WhiteLabelClient client) {
        WhiteLabel.client = (client != null) ? client : new NoOpWhiteLabelClient();
    }

    public static WhiteLabelTheme atual() {
        return client.buscar(TenantContext.get());
    }

    public static WhiteLabelTheme de(String tenantId) {
        return client.buscar(tenantId);
    }
}
