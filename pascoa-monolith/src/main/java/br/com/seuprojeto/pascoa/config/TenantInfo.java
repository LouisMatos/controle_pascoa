package br.com.seuprojeto.pascoa.config;

/**
 * V9 Etapa 1.4 — Snapshot de identidade do tenant exposto ao Thymeleaf como
 * ${tenant}. Os campos casam com o que o {@code fragments/layout.html} acessa
 * (id, corPrimaria, corPrimariaDark, corSecundaria, faviconUrl, nomeApp, ...).
 *
 * No monólito single-tenant (operação atual = pascoa-original) o
 * {@link GlobalModelAdvice} injeta {@link #defaultTenant()} para que nenhum
 * template quebre por NPE em campos do tenant.
 */
public record TenantInfo(
        String id,
        String nomeApp,
        String corPrimaria,
        String corPrimariaDark,
        String corSecundaria,
        String logoUrl,
        String faviconUrl,
        String rodape,
        String plano,
        String whatsapp
) {
    public static TenantInfo defaultTenant() {
        return new TenantInfo(
                "pascoa-original",
                "Controle Páscoa",
                "#e91e63",
                "#c2185b",
                "#fce4ec",
                "/images/logo-default.svg",
                "/icons/icon-192.png",
                "Controle Páscoa — Gestão de Ovos de Páscoa Artesanal",
                "ENTERPRISE",
                ""
        );
    }
}
