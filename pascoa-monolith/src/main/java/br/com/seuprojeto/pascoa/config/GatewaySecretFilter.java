package br.com.seuprojeto.pascoa.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * No sandbox AWS a EC2 fica com a porta da aplicação aberta na internet, porque a integração
 * HTTP_PROXY do API Gateway não tem faixa de IP fixa. Este filtro garante que só o tráfego
 * vindo do API Gateway (que injeta X-Gateway-Secret) chegue à aplicação.
 * Segredo vazio = filtro inativo (ambiente local).
 */
@Component
@Order(-200)
public class GatewaySecretFilter extends OncePerRequestFilter {

    private final byte[] segredo;

    public GatewaySecretFilter(@Value("${app.gateway.shared-secret:}") String segredo) {
        this.segredo = segredo == null ? new byte[0] : segredo.trim().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // healthcheck do container bate em localhost sem passar pelo gateway
        return segredo.length == 0 || request.getRequestURI().startsWith("/actuator/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String recebido = request.getHeader("X-Gateway-Secret");
        if (recebido == null
                || !MessageDigest.isEqual(segredo, recebido.getBytes(StandardCharsets.UTF_8))) {
            // sendError() dispara dispatch ERROR para /error, que exige autenticação e
            // acabaria em 302 para /login. Resposta escrita direto evita o redirect.
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("403 Forbidden");
            return;
        }
        chain.doFilter(request, response);
    }
}
