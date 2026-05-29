package br.com.seuprojeto.pascoa.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.util.Optional;

/**
 * Substitui o {@link LockProvider} do ShedLock por uma implementação no-op no perfil "test".
 *
 * <p>Nos testes, a tabela {@code shedlock} não existe (Flyway desabilitado + H2 criado
 * pelo Hibernate). O provider no-op garante que métodos anotados com {@code @SchedulerLock}
 * possam ser chamados diretamente sem tentar acessar o banco de dados.
 *
 * <p>{@code @Primary} sobrepõe o bean real registrado por {@link ShedLockConfig}.
 * {@code spring.main.allow-bean-definition-overriding=true} é necessário no profile test.
 */
@Configuration
@Profile("test")
public class TestShedLockConfig {

    @Bean
    @Primary
    public LockProvider noOpLockProvider() {
        return lockConfiguration -> Optional.of(new SimpleLock() {
            @Override
            public void unlock() { /* no-op */ }
        });
    }
}
