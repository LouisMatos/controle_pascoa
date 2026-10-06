package br.com.seuprojeto.pascoa.config;

import br.com.seuprojeto.pascoa.seguranca.entity.Loja;
import br.com.seuprojeto.pascoa.seguranca.entity.Role;
import br.com.seuprojeto.pascoa.seguranca.entity.Usuario;
import br.com.seuprojeto.pascoa.seguranca.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.senha-inicial:admin123}")
    private String senhaInicial;

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() == 0) {
            usuarioRepository.save(Usuario.builder()
                .nome("Administrador")
                .login("admin")
                .senha(passwordEncoder.encode(senhaInicial))
                .role(Role.ADMIN)
                .ativo(true)
                .lojaId(Loja.PLATAFORMA_ID)
                .build());
            log.info("=== Usuário inicial criado: login=admin (senha em app.admin.senha-inicial) ===");
        }
    }
}
