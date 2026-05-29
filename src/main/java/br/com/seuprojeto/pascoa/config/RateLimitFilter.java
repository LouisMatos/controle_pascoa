package br.com.seuprojeto.pascoa.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting por IP usando Bucket4j (in-memory, por instância).
 *
 * Limites:
 *  - /catalogo/**              → 60 req/min
 *  - /acompanhamento/**
 *    /orcamento-publico/**     → 10 req/min
 *  - POST /login               → 5 tentativas/15 min (bloqueio automático)
 */
@Component
@Order(-101) // executa antes do Spring Security (order = -100)
public class RateLimitFilter extends OncePerRequestFilter {

    private enum Category { CATALOGO, TRACKING, LOGIN, NONE }

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        Category category = categorize(request);
        if (category == Category.NONE) {
            chain.doFilter(request, response);
            return;
        }

        String key = ip(request) + ":" + category.name();
        Bucket bucket = buckets.computeIfAbsent(key, k -> buildBucket(category));

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            long waitSeconds = bucket.getAvailableTokens() == 0
                ? refillSeconds(category)
                : 60;
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(waitSeconds));
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write(
                "Muitas requisições. Tente novamente em " + waitSeconds + " segundos.");
        }
    }

    private Category categorize(HttpServletRequest req) {
        String path = req.getRequestURI();
        String method = req.getMethod();

        if ("POST".equalsIgnoreCase(method) && "/login".equals(path)) return Category.LOGIN;
        if (path.startsWith("/catalogo"))                               return Category.CATALOGO;
        if (path.startsWith("/acompanhamento") ||
            path.startsWith("/orcamento-publico"))                      return Category.TRACKING;
        return Category.NONE;
    }

    private Bucket buildBucket(Category category) {
        Bandwidth limit = switch (category) {
            case CATALOGO -> Bandwidth.classic(60,  Refill.intervally(60,  Duration.ofMinutes(1)));
            case TRACKING -> Bandwidth.classic(10,  Refill.intervally(10,  Duration.ofMinutes(1)));
            case LOGIN    -> Bandwidth.classic(5,   Refill.intervally(5,   Duration.ofMinutes(15)));
            case NONE     -> throw new IllegalStateException();
        };
        return Bucket.builder().addLimit(limit).build();
    }

    private long refillSeconds(Category category) {
        return switch (category) {
            case LOGIN    -> 15 * 60L;
            case TRACKING -> 60L;
            default       -> 60L;
        };
    }

    private String ip(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
