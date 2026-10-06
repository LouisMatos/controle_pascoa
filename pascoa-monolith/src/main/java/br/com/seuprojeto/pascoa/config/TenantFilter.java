package br.com.seuprojeto.pascoa.config;

import br.com.seuprojeto.pascoa.common.tenant.TenantContext;
import br.com.seuprojeto.pascoa.seguranca.entity.Loja;
import br.com.seuprojeto.pascoa.seguranca.service.UsuarioPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    private static final String SQL_PEDIDO = "SELECT loja_id FROM pedidos WHERE token_acompanhamento = ?";
    private static final String SQL_ORCAMENTO = "SELECT loja_id FROM orcamentos WHERE token_aprovacao = ?";

    private final JdbcTemplate jdbc;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Long lojaId = resolverLoja(request);
        if (lojaId == null) {
            chain.doFilter(request, response);
            return;
        }
        try (var escopo = TenantContext.abrir(lojaId)) {
            chain.doFilter(request, response);
        }
    }

    private Long resolverLoja(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioPrincipal principal) {
            return principal.getLojaId();
        }
        String[] partes = request.getServletPath().split("/");
        if (partes.length >= 3 && "acompanhamento".equals(partes[1])) {
            return primeiro(SQL_PEDIDO, partes[2]);
        }
        if (partes.length >= 3 && "orcamento-publico".equals(partes[1])) {
            return primeiro(SQL_ORCAMENTO, partes[2]);
        }
        if (partes.length >= 2 && "catalogo".equals(partes[1])) {
            return Loja.PLATAFORMA_ID;
        }
        return null;
    }

    private Long primeiro(String sql, String token) {
        List<Long> ids = jdbc.queryForList(sql, Long.class, token);
        return ids.isEmpty() ? null : ids.get(0);
    }
}
