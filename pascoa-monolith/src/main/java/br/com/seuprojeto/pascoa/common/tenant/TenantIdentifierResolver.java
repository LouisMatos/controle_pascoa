package br.com.seuprojeto.pascoa.common.tenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<Long> {

    @Override
    public Long resolveCurrentTenantIdentifier() {
        return TenantContext.atual();
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }
}
