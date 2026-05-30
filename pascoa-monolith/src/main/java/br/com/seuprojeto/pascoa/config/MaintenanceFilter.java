package br.com.seuprojeto.pascoa.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Intercepta todas as requisições quando o modo manutenção está ativo.
 * Usuários com ROLE_ADMIN passam livremente; os demais são redirecionados para /manutencao.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class MaintenanceFilter extends OncePerRequestFilter {

    private final ConfiguracaoSistemaRepository sistemaRepository;

    /** Caminhos que nunca são bloqueados, mesmo em manutenção. */
    private static final List<String> ALLOWED_PREFIXES = List.of(
        "/manutencao",
        "/login",
        "/logout",
        "/2fa",
        "/actuator",
        "/icons",
        "/css",
        "/js",
        "/manifest.json",
        "/sw.js",
        "/favicon"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getServletPath();

        // Sempre libera paths de suporte
        if (isAllowedPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Lê configuração (consulta leve — linha única no banco)
        boolean emManutencao = sistemaRepository.findById(1L)
            .map(c -> Boolean.TRUE.equals(c.getModoManutencao()))
            .orElse(false);

        if (!emManutencao) {
            filterChain.doFilter(request, response);
            return;
        }

        // Verifica se o usuário autenticado é ADMIN
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null
            && auth.isAuthenticated()
            && !"anonymousUser".equals(auth.getPrincipal())
            && auth.getAuthorities().stream()
                   .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        if (isAdmin) {
            filterChain.doFilter(request, response);
            return;
        }

        // Modo manutenção ativo e usuário não é ADMIN → redireciona
        log.debug("[MANUTENCAO] Bloqueando acesso de {} a {}", path,
            auth != null ? auth.getName() : "anônimo");
        response.sendRedirect(request.getContextPath() + "/manutencao");
    }

    private boolean isAllowedPath(String path) {
        return ALLOWED_PREFIXES.stream().anyMatch(path::startsWith);
    }
}
