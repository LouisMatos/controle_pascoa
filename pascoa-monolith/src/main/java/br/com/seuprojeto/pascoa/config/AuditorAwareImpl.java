package br.com.seuprojeto.pascoa.config;

import br.com.seuprojeto.pascoa.shared.SecurityUtils;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Fornece o usuário atual ao Spring Data Auditing (@CreatedBy / @LastModifiedBy).
 * Retorna "sistema" quando não há sessão autenticada (ex.: tarefas batch, testes).
 */
@Component("auditorAwareImpl")
public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        return Optional.of(SecurityUtils.login("sistema"));
    }
}
