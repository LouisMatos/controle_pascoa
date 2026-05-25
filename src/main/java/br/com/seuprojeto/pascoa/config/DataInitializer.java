package br.com.seuprojeto.pascoa.config;

import br.com.seuprojeto.pascoa.seguranca.entity.Role;
import br.com.seuprojeto.pascoa.seguranca.entity.Usuario;
import br.com.seuprojeto.pascoa.seguranca.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() == 0) {
            usuarioRepository.save(Usuario.builder()
                .nome("Administrador")
                .login("admin")
                .senha(passwordEncoder.encode("admin123"))
                .role(Role.ADMIN)
                .ativo(true)
                .build());
            log.info("=== Usuário inicial criado: login=admin | senha=admin123 ===");
        }
    }
}
