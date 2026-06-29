package br.com.seuprojeto.pascoa.commons.tenant;

/**
 * ThreadLocal com o tenantId da requisição atual.
 * Setado pelo TenantSchemaInterceptor no início do request e limpo no afterCompletion.
 * Quando vazio, o {@link #DEFAULT_TENANT} é usado como fallback (compat single-tenant v5).
 */
public final class TenantContext {

    public static final String HEADER_NAME = "X-Tenant-Id";
    public static final String DEFAULT_TENANT = "public";

    private static final ThreadLocal<String> TENANT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(String tenantId) {
        TENANT.set(tenantId);
    }

    public static String get() {
        String t = TENANT.get();
        return t != null ? t : DEFAULT_TENANT;
    }

    public static String getOrNull() {
        return TENANT.get();
    }

    public static void clear() {
        TENANT.remove();
    }
}
