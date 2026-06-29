package br.com.seuprojeto.pascoa.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * v6 Etapa 12 — Rate limiting por tenant.
 *
 * <p>Usamos o {@code RequestRateLimiter} pronto do Spring Cloud Gateway (algoritmo
 * token-bucket no Redis) e provemos um {@link KeyResolver} customizado: a chave é
 * o header {@code X-Tenant-Id} (já populado pelo {@link
 * br.com.seuprojeto.pascoa.gateway.filter.TenantResolutionFilter}); requests
 * sem tenant caem em uma chave compartilhada {@code anonymous}.
 *
 * <p>Configuração de bursts/rate fica em {@code application.yml} via
 * {@code spring.cloud.gateway.default-filters} (ou por rota).
 */
@Configuration
public class RateLimitConfig {

    public static final String ANONYMOUS_KEY = "anonymous";

    @Bean("tenantKeyResolver")
    public KeyResolver tenantKeyResolver() {
        return exchange -> {
            String tenantId = exchange.getRequest().getHeaders().getFirst("X-Tenant-Id");
            return Mono.just((tenantId != null && !tenantId.isBlank()) ? tenantId : ANONYMOUS_KEY);
        };
    }
}
