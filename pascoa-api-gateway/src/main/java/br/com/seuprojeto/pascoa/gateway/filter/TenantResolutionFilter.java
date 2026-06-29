package br.com.seuprojeto.pascoa.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * v6 Etapa 12 — Resolve o {@code X-Tenant-Id} a partir do subdomínio do {@code Host}
 * e propaga para os microsserviços downstream.
 *
 * <h3>Regras de resolução</h3>
 * <ol>
 *   <li>Se o request já trouxer {@code X-Tenant-Id} (testes, gateway-para-gateway),
 *       respeitamos o valor.</li>
 *   <li>Caso o path case com um {@code foodflow.tenant.bypass-paths} (default:
 *       {@code /actuator/**, /webhooks/**, /fallback/**}), pula a resolução.</li>
 *   <li>{@code Host} = {@code <slug>.foodflow.com.br} → tenantId = {@code <slug>}.</li>
 *   <li>{@code Host} = localhost / IP / sem subdomínio reconhecível → não injeta header
 *       (modo legacy v5, mantém compat com o monólito).</li>
 * </ol>
 *
 * <p>Roda com precedência maior (=número menor) que {@link RequestTracingFilter} para que
 * o header esteja disponível ao primeiro log.
 */
@Slf4j
@Component
public class TenantResolutionFilter implements GlobalFilter, Ordered {

    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Value("${foodflow.tenant.base-domain:foodflow.com.br}")
    private String baseDomain;

    @Value("${foodflow.tenant.bypass-paths:/actuator/**,/webhooks/**,/fallback/**}")
    private List<String> bypassPaths;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest req = exchange.getRequest();
        String path = req.getPath().value();

        for (String pattern : bypassPaths) {
            if (PATH_MATCHER.match(pattern.trim(), path)) {
                return chain.filter(exchange);
            }
        }

        String existing = req.getHeaders().getFirst(TENANT_HEADER);
        if (existing != null && !existing.isBlank()) {
            return chain.filter(exchange);
        }

        String host = req.getHeaders().getFirst("X-Forwarded-Host");
        if (host == null || host.isBlank()) {
            host = req.getURI().getHost();
        }
        String tenantId = resolverTenantId(host);

        if (tenantId == null) {
            log.trace("Sem tenant identificado para host={}", host);
            return chain.filter(exchange);
        }

        log.debug("Tenant resolvido: host={} → {}", host, tenantId);
        ServerHttpRequest mutated = req.mutate()
                .header(TENANT_HEADER, tenantId)
                .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    /** Extrai o slug do tenant do host: {@code confeitaria-maria.foodflow.com.br} → {@code confeitaria-maria}. */
    String resolverTenantId(String host) {
        if (host == null || host.isBlank()) return null;
        host = host.toLowerCase();
        int colon = host.indexOf(':');
        if (colon > 0) host = host.substring(0, colon);

        // localhost / IPv4 / nada de subdomínio → modo legacy v5
        if (host.equals("localhost") || host.matches("\\d+(\\.\\d+){3}")) return null;

        if (host.endsWith("." + baseDomain)) {
            String slug = host.substring(0, host.length() - baseDomain.length() - 1);
            // pegar o primeiro segmento — `app.foodflow.com.br` → "app" (mas reservamos esse?)
            int dot = slug.indexOf('.');
            return (dot >= 0 ? slug.substring(0, dot) : slug);
        }

        // Domínio custom (ENTERPRISE) — não resolvemos automaticamente; tenant-service teria de mapear.
        return null;
    }

    @Override
    public int getOrder() {
        // Antes do RequestTracingFilter para o log já ter X-Tenant-Id.
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
