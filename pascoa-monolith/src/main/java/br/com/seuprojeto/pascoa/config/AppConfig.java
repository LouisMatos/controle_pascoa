package br.com.seuprojeto.pascoa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import br.com.seuprojeto.pascoa.common.tenant.TenantAwareRepository;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

/**
 * Beans de infraestrutura da aplicação:
 * - RestTemplate para chamadas HTTP externas (Evolution API / WhatsApp)
 * - @EnableAsync habilita o processamento assíncrono de eventos (@Async)
 */
@Configuration
@EnableAsync
@EnableJpaAuditing(auditorAwareRef = "auditorAwareImpl")
@EnableJpaRepositories(basePackages = "br.com.seuprojeto.pascoa", repositoryBaseClass = TenantAwareRepository.class)
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
