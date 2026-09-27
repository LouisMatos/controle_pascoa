package br.com.seuprojeto.pascoa.auth.config;

import br.com.seuprojeto.pascoa.auth.application.port.out.TokenBlacklistPort;
import br.com.seuprojeto.pascoa.auth.domain.exception.TokenException;
import br.com.seuprojeto.pascoa.auth.domain.service.JwtDomainService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtDomainService jwtService;
    private final TokenBlacklistPort tokenBlacklist;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        try {
            if (tokenBlacklist.isBlacklisted(token)) throw TokenException.blacklisted();
            Claims claims = jwtService.parse(token);
            if (!"access".equals(claims.get("type"))) throw TokenException.invalid();

            List<String> roles = claims.get("roles", List.class);
            var authorities = roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            var auth = new UsernamePasswordAuthenticationToken(
                    claims.get("login"), null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (TokenException e) {
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(request, response);
    }
}
