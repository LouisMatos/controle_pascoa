package br.com.seuprojeto.pascoa.common.tenant;

import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TenantHibernateConfig {

    @Bean
    public HibernatePropertiesCustomizer tenantResolverCustomizer(TenantIdentifierResolver resolver) {
        return props -> props.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, resolver);
    }
}
