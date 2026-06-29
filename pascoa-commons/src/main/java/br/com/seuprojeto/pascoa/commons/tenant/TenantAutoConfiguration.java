package br.com.seuprojeto.pascoa.commons.tenant;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Auto-configuração da infraestrutura multi-tenant.
 *
 * <p>Ativada via property {@code foodflow.tenant.enabled=true}.
 * <p>Quando ativa:
 * <ul>
 *   <li>Cria um {@link TenantAwareDataSource} envolvendo o {@link DataSource} padrão do Spring Boot,
 *       que executa {@code SET search_path TO <tenant>, public} em cada conexão.</li>
 *   <li>Registra o {@link TenantSchemaInterceptor} no pipeline do Spring MVC, lendo o header
 *       {@code X-Tenant-Id} injetado pelo API Gateway.</li>
 * </ul>
 *
 * <p>Quando desativada (default), os 9 serviços v5 continuam funcionando em modo single-tenant
 * sem qualquer alteração de comportamento. Esse é o gate de compatibilidade durante a transição v5→v6.
 */
@AutoConfiguration(before = DataSourceAutoConfiguration.class)
@ConditionalOnProperty(prefix = "foodflow.tenant", name = "enabled", havingValue = "true")
public class TenantAutoConfiguration {

    @Configuration
    @ConditionalOnClass(name = "jakarta.servlet.Servlet")
    static class WebMvcTenantConfig implements WebMvcConfigurer {
        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(new TenantSchemaInterceptor());
        }
    }

    @Configuration
    static class DataSourceTenantConfig {

        @Bean
        @ConfigurationProperties("spring.datasource")
        public DataSource tenantTargetDataSource() {
            return DataSourceBuilder.create().build();
        }

        @Bean
        @Primary
        public DataSource dataSource(@Autowired DataSource tenantTargetDataSource) {
            return new TenantAwareDataSource(tenantTargetDataSource);
        }
    }
}
