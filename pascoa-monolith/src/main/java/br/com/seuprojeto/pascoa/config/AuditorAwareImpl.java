package br.com.seuprojeto.pascoa.config;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Fornece o usuário atual ao Spring Data Auditing (@CreatedBy / @LastModifiedBy).
 * Retorna "sistema" quando não há sessão autenticada (ex.: tarefas batch, testes).
 */
@Component("auditorAwareImpl")
public class AuditorAwareImpl implements AuditorAware<String> {

    private static final String SISTEMA = "sistema";

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getName())) {
            return Optional.of(auth.getName());
        }
        return Optional.of(SISTEMA);
    }
}
