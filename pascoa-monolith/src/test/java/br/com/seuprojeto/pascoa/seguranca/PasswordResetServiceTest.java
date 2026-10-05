package br.com.seuprojeto.pascoa.seguranca;

import br.com.seuprojeto.pascoa.seguranca.entity.PasswordResetToken;
import br.com.seuprojeto.pascoa.seguranca.entity.Role;
import br.com.seuprojeto.pascoa.seguranca.entity.Usuario;
import br.com.seuprojeto.pascoa.seguranca.repository.PasswordResetTokenRepository;
import br.com.seuprojeto.pascoa.seguranca.repository.UsuarioRepository;
import br.com.seuprojeto.pascoa.seguranca.service.PasswordResetService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes de integração para {@link PasswordResetService}.
 *
 * <p>Cobre:
 * <ul>
 *   <li>Geração de token e envio (silencioso em caso de falha SMTP)</li>
 *   <li>Validação: token válido, expirado, já usado</li>
 *   <li>Reset de senha: senha atualizada, token marcado como usado</li>
 *   <li>Idempotência: token anterior apagado ao solicitar novo reset</li>
 *   <li>Anti-enumeração: usuário inexistente, inativo, sem e-mail</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PasswordResetServiceTest {

    @Autowired private PasswordResetService       resetService;
    @Autowired private PasswordResetTokenRepository tokenRepository;
    @Autowired private UsuarioRepository          usuarioRepository;
    @Autowired private PasswordEncoder            passwordEncoder;
    @Autowired private EntityManager              em;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = usuarioRepository.save(Usuario.builder()
                .nome("Usuário Reset")
                .login("reset.user")
                .senha(passwordEncoder.encode("senhaOriginal"))
                .role(Role.ATENDENTE)
                .email("reset@pascoa.local")
                .ativo(true)
                .build());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // solicitarReset
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Solicitar reset por login válido → retorna true e cria token")
    void solicitar_loginValido_criaToken() {
        boolean ok = resetService.solicitarReset("reset.user");

        assertThat(ok).isTrue();
        assertThat(tokenRepository.findAll())
                .anyMatch(t -> t.getUsuario().getId().equals(usuario.getId()));
    }

    @Test
    @DisplayName("Solicitar reset por e-mail válido → retorna true e cria token")
    void solicitar_emailValido_criaToken() {
        boolean ok = resetService.solicitarReset("reset@pascoa.local");

        assertThat(ok).isTrue();
        assertThat(tokenRepository.findAll())
                .anyMatch(t -> t.getUsuario().getId().equals(usuario.getId()));
    }

    @Test
    @DisplayName("Solicitar reset com login inexistente → retorna false (anti-enumeração)")
    void solicitar_loginInexistente_retornaFalse() {
        boolean ok = resetService.solicitarReset("nao.existe");

        assertThat(ok).isFalse();
        assertThat(tokenRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Solicitar reset para usuário inativo → retorna false")
    void solicitar_usuarioInativo_retornaFalse() {
        usuario.setAtivo(false);
        usuarioRepository.save(usuario);
        em.flush(); em.clear();

        boolean ok = resetService.solicitarReset("reset.user");

        assertThat(ok).isFalse();
    }

    @Test
    @DisplayName("Solicitar reset para usuário sem e-mail → retorna false")
    void solicitar_semEmail_retornaFalse() {
        usuario.setEmail(null);
        usuarioRepository.save(usuario);
        em.flush(); em.clear();

        boolean ok = resetService.solicitarReset("reset.user");

        assertThat(ok).isFalse();
    }

    @Test
    @DisplayName("Segunda solicitação apaga o token anterior do mesmo usuário")
    void solicitar_duasVezes_tokenAnteriorApagado() {
        resetService.solicitarReset("reset.user");
        em.flush(); em.clear();

        // Primeira solicitação gerou 1 token
        assertThat(tokenRepository.findAll()).hasSize(1);
        String primeiroToken = tokenRepository.findAll().get(0).getToken();

        resetService.solicitarReset("reset.user");
        em.flush(); em.clear();

        // Deve haver apenas 1 token (o novo; o anterior foi apagado)
        assertThat(tokenRepository.findAll()).hasSize(1);
        String segundoToken = tokenRepository.findAll().get(0).getToken();
        assertThat(segundoToken).isNotEqualTo(primeiroToken);
    }

    @Test
    @DisplayName("Token gerado tem expiração futura (~30 min)")
    void solicitar_tokenTemExpiracaoFutura() {
        resetService.solicitarReset("reset.user");
        em.flush(); em.clear();

        PasswordResetToken token = tokenRepository.findAll().get(0);

        assertThat(token.getExpiraEm()).isAfter(LocalDateTime.now());
        assertThat(token.getExpiraEm()).isBefore(LocalDateTime.now().plusMinutes(31));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // validarToken
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("validarToken com token válido retorna Optional não-vazio")
    void validar_tokenValido_retornaPresente() {
        resetService.solicitarReset("reset.user");
        em.flush(); em.clear();
        String valorToken = tokenRepository.findAll().get(0).getToken();

        Optional<PasswordResetToken> resultado = resetService.validarToken(valorToken);

        assertThat(resultado).isPresent();
    }

    @Test
    @DisplayName("validarToken com token inexistente retorna Optional vazio")
    void validar_tokenInexistente_retornaVazio() {
        Optional<PasswordResetToken> resultado = resetService.validarToken("nao-existe-uuid");

        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("validarToken com token expirado retorna Optional vazio")
    void validar_tokenExpirado_retornaVazio() {
        // Cria token diretamente com expiração no passado
        PasswordResetToken tokenExpirado = PasswordResetToken.builder()
                .usuario(usuario)
                .token("token-expirado-abc123")
                .expiraEm(LocalDateTime.now().minusMinutes(1))
                .build();
        tokenRepository.save(tokenExpirado);
        em.flush(); em.clear();

        Optional<PasswordResetToken> resultado = resetService.validarToken("token-expirado-abc123");

        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("validarToken com token já usado retorna Optional vazio")
    void validar_tokenUsado_retornaVazio() {
        PasswordResetToken tokenUsado = PasswordResetToken.builder()
                .usuario(usuario)
                .token("token-ja-usado-xyz")
                .expiraEm(LocalDateTime.now().plusMinutes(30))
                .usado(true)
                .build();
        tokenRepository.save(tokenUsado);
        em.flush(); em.clear();

        Optional<PasswordResetToken> resultado = resetService.validarToken("token-ja-usado-xyz");

        assertThat(resultado).isEmpty();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // resetarSenha
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("resetarSenha com token válido → retorna true, atualiza senha e marca token como usado")
    void resetar_tokenValido_atualizaSenhaEMarcaUsado() {
        resetService.solicitarReset("reset.user");
        em.flush(); em.clear();
        String valorToken = tokenRepository.findAll().get(0).getToken();

        boolean ok = resetService.resetarSenha(valorToken, "novaSenha123");
        em.flush(); em.clear();

        assertThat(ok).isTrue();

        // Senha atualizada
        Usuario atualizado = usuarioRepository.findById(usuario.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("novaSenha123", atualizado.getSenha())).isTrue();

        // Token marcado como usado
        PasswordResetToken token = tokenRepository.findByToken(valorToken).orElseThrow();
        assertThat(token.getUsado()).isTrue();
    }

    @Test
    @DisplayName("resetarSenha com token inexistente → retorna false")
    void resetar_tokenInexistente_retornaFalse() {
        boolean ok = resetService.resetarSenha("token-invalido-xyz", "qualquerSenha");

        assertThat(ok).isFalse();
    }

    @Test
    @DisplayName("Usar o mesmo token duas vezes → segunda chamada retorna false")
    void resetar_mesmoBilheteduasVezes_segundaFalha() {
        resetService.solicitarReset("reset.user");
        em.flush(); em.clear();
        String valorToken = tokenRepository.findAll().get(0).getToken();

        boolean primeiraVez = resetService.resetarSenha(valorToken, "primeiraNova1");
        em.flush(); em.clear();
        boolean segundaVez = resetService.resetarSenha(valorToken, "segundaNova2");

        assertThat(primeiraVez).isTrue();
        assertThat(segundaVez).isFalse(); // token já está marcado como usado
    }
}
