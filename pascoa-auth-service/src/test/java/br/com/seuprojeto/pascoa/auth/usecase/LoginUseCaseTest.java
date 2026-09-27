package br.com.seuprojeto.pascoa.auth.usecase;

import br.com.seuprojeto.pascoa.auth.application.port.in.AuthUseCase.LoginCommand;
import br.com.seuprojeto.pascoa.auth.application.port.out.AuthEventPublisherPort;
import br.com.seuprojeto.pascoa.auth.application.port.out.TokenBlacklistPort;
import br.com.seuprojeto.pascoa.auth.application.port.out.UserRepositoryPort;
import br.com.seuprojeto.pascoa.auth.application.usecase.AuthUseCaseImpl;
import br.com.seuprojeto.pascoa.auth.domain.exception.AccountBlockedException;
import br.com.seuprojeto.pascoa.auth.domain.exception.InvalidCredentialsException;
import br.com.seuprojeto.pascoa.auth.domain.model.Role;
import br.com.seuprojeto.pascoa.auth.domain.model.Token;
import br.com.seuprojeto.pascoa.auth.domain.model.Usuario;
import br.com.seuprojeto.pascoa.auth.domain.service.JwtDomainService;
import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

    @Mock UserRepositoryPort userRepository;
    @Mock TokenBlacklistPort tokenBlacklist;
    @Mock AuthEventPublisherPort eventPublisher;

    private AuthUseCaseImpl useCase;
    private BCryptPasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        encoder = new BCryptPasswordEncoder();
        JwtDomainService jwtService = new JwtDomainService(
                "chave-secreta-teste-minimo-32-caracteres-ok",
                Duration.ofMinutes(15),
                Duration.ofDays(7)
        );
        useCase = new AuthUseCaseImpl(
                userRepository, tokenBlacklist, eventPublisher,
                jwtService, encoder, new TimeBasedOneTimePasswordGenerator()
        );
    }

    @Test
    @DisplayName("Login com credenciais válidas retorna token")
    void loginSucesso() {
        Usuario usuario = usuario("admin123");
        when(userRepository.findByLogin("admin")).thenReturn(Optional.of(usuario));
        when(userRepository.save(any())).thenReturn(usuario);

        Token token = useCase.login(new LoginCommand("admin", "admin123", null));

        assertThat(token.getAccessToken()).isNotBlank();
        assertThat(token.getRefreshToken()).isNotBlank();
        assertThat(token.getAccessExpiresAt()).isAfter(Instant.now());
        verify(eventPublisher).publishLoginSuccess(1L, "admin");
    }

    @Test
    @DisplayName("Login com senha errada lança InvalidCredentialsException")
    void loginSenhaErrada() {
        Usuario usuario = usuario("admin123");
        when(userRepository.findByLogin("admin")).thenReturn(Optional.of(usuario));
        when(userRepository.save(any())).thenReturn(usuario.incrementarFalhas());

        assertThatThrownBy(() -> useCase.login(new LoginCommand("admin", "senha-errada", null)))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(eventPublisher).publishLoginFailed("admin", "senha_incorreta");
    }

    @Test
    @DisplayName("Login com usuário inexistente lança InvalidCredentialsException")
    void loginUsuarioNaoEncontrado() {
        when(userRepository.findByLogin("fantasma")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.login(new LoginCommand("fantasma", "qualquer", null)))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("Login com conta bloqueada lança AccountBlockedException")
    void loginContaBloqueada() {
        Usuario bloqueado = usuario("admin123").withTentativasFalhas(5);
        when(userRepository.findByLogin("admin")).thenReturn(Optional.of(bloqueado));

        assertThatThrownBy(() -> useCase.login(new LoginCommand("admin", "admin123", null)))
                .isInstanceOf(AccountBlockedException.class);
    }

    private Usuario usuario(String senha) {
        return Usuario.builder()
                .id(1L)
                .login("admin")
                .passwordHash(encoder.encode(senha))
                .roles(Set.of(Role.ADMIN))
                .ativo(true)
                .totpAtivado(false)
                .tentativasFalhas(0)
                .build();
    }
}
