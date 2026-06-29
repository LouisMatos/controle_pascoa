package br.com.seuprojeto.pascoa.commons.whitelabel;

/**
 * Snapshot imutável da configuração visual do tenant — replica de
 * {@code platform.white_label_config}. Compartilhado por todos os serviços
 * que precisam renderizar UI ou exportar PDF com identidade do tenant.
 */
public record WhiteLabelTheme(
        String tenantId,
        String nomeApp,
        String logoUrl,
        String corPrimaria,
        String corSecundaria,
        String favicon,
        String rodape
) {
    public static final WhiteLabelTheme DEFAULT = new WhiteLabelTheme(
            null, "FoodFlow", null, "#1976d2", "#90caf9", null, null);
}
