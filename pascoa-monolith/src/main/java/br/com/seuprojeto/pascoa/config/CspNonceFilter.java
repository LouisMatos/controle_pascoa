package br.com.seuprojeto.pascoa.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * S-01 — gera um nonce CSP por request, expõe via request attribute
 * {@code cspNonce} (consumido pelos templates Thymeleaf via {@code th:nonce})
 * e injeta no header {@code Content-Security-Policy}.
 *
 * O CSP atual mantém {@code 'unsafe-inline'} para compatibilidade com os
 * inúmeros {@code style="..."} inline do código legado (atributo coberto
 * por {@code style-src-attr}, separado da política para {@code <style>}).
 * Browsers que entendem {@code 'nonce-...'} ignoram {@code 'unsafe-inline'}
 * para tags {@code <style>} marcadas com o nonce — defesa em profundidade
 * que permite migração gradual sem quebra.
 *
 * O bloco {@code .contentSecurityPolicy(...)} do Spring Security continua
 * setando o header default; este filter sobrescreve quando há nonce.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class CspNonceFilter extends OncePerRequestFilter {

    public static final String REQUEST_ATTR = "cspNonce";

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        req.setAttribute(REQUEST_ATTR, nonce);

        // Sobrescreve a CSP estática do SecurityConfig com a versão com nonce.
        // Inclui 'unsafe-inline' como fallback durante a migração.
        String csp =
                "default-src 'self'; " +
                "script-src 'self' 'nonce-" + nonce + "' cdn.jsdelivr.net; " +
                "style-src 'self' 'nonce-" + nonce + "' 'unsafe-inline' cdn.jsdelivr.net; " +
                "font-src 'self' cdn.jsdelivr.net; " +
                "img-src 'self' data:; " +
                "connect-src 'self'; " +
                "frame-ancestors 'none'";
        res.setHeader("Content-Security-Policy", csp);

        chain.doFilter(req, res);
    }
}
