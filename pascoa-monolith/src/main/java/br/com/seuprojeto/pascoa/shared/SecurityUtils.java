package br.com.seuprojeto.pascoa.shared;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static boolean autenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null
            && auth.isAuthenticated()
            && !(auth instanceof AnonymousAuthenticationToken);
    }

    /** Login do usuário atual, ou o fallback quando não há sessão (jobs, testes). */
    public static String login(String fallback) {
        return autenticado()
            ? SecurityContextHolder.getContext().getAuthentication().getName()
            : fallback;
    }
}
