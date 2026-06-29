package br.com.seuprojeto.pascoa.tenant.exception;

public class TenantNotFoundException extends TenantException {
    public TenantNotFoundException(String tenantId) {
        super("Tenant não encontrado: " + tenantId);
    }
}
