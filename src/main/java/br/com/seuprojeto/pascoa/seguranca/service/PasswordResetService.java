package br.com.seuprojeto.pascoa.seguranca.service;

import br.com.seuprojeto.pascoa.seguranca.entity.PasswordResetToken;
import br.com.seuprojeto.pascoa.seguranca.entity.Usuario;
import br.com.seuprojeto.pascoa.seguranca.repository.PasswordResetTokenRepository;
import br.com.seuprojeto.pascoa.seguranca.repository.UsuarioRepository;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Gerencia o fluxo de recuperação de senha:
 * <ol>
 *   <li>Usuário informa login ou e-mail → token UUID gerado e link enviado por e-mail</li>
 *   <li>Usuário clica no link → token validado (não usado + não expirado)</li>
 *   <li>Usuário define nova senha → token marcado como usado, senha atualizada</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private static final int EXPIRACAO_MINUTOS = 30;

    private final PasswordResetTokenRepository tokenRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${spring.mail.username:noreply@pascoa.local}")
    private String remetente;

    // -----------------------------------------------------------------------
    // Solicitar reset
    // -----------------------------------------------------------------------

    /**
     * Localiza o usuário pelo login ou e-mail, gera um token e envia o e-mail.
     * Retorna {@code true} se o e-mail foi (tentado) enviar,
     * {@code false} se o usuário não existe, está inativo ou não tem e-mail cadastrado.
     * <p>
     * Para evitar enumeração de usuários, o controller sempre exibe a mesma mensagem
     * ao usuário, independentemente do retorno.
     */
    @Transactional
    public boolean solicitarReset(String loginOuEmail) {
        // Busca por login; se não achar, tenta pelo e-mail
        Optional<Usuario> optUsuario = usuarioRepository.findByLogin(loginOuEmail);
        if (optUsuario.isEmpty()) {
            optUsuario = usuarioRepository.findByEmail(loginOuEmail);
        }
        if (optUsuario.isEmpty()) {
            log.warn("[RESET] Usuário não encontrado para: {}", loginOuEmail);
            return false;
        }

        Usuario usuario = optUsuario.get();

        if (!usuario.isAtivo()) {
            log.warn("[RESET] Usuário inativo: {}", usuario.getLogin());
            return false;
        }
        if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {
            log.warn("[RESET] Usuário sem e-mail cadastrado: {}", usuario.getLogin());
            return false;
        }

        // Invalida tokens anteriores do mesmo usuário
        tokenRepository.deleteByUsuarioId(usuario.getId());

        // Cria novo token com expiração em 30 min
        String valorToken = UUID.randomUUID().toString();
        PasswordResetToken token = PasswordResetToken.builder()
            .usuario(usuario)
            .token(valorToken)
            .expiraEm(LocalDateTime.now().plusMinutes(EXPIRACAO_MINUTOS))
            .build();
        tokenRepository.save(token);

        // Envia e-mail (falha silenciosa — token já foi criado)
        enviarEmailReset(usuario, valorToken);
        return true;
    }

    // -----------------------------------------------------------------------
    // Validar token
    // -----------------------------------------------------------------------

    /**
     * Retorna o token se for válido (não usado e não expirado), ou vazio caso contrário.
     */
    @Transactional(readOnly = true)
    public Optional<PasswordResetToken> validarToken(String valorToken) {
        return tokenRepository.findByToken(valorToken)
            .filter(PasswordResetToken::isValido);
    }

    // -----------------------------------------------------------------------
    // Redefinir senha
    // -----------------------------------------------------------------------

    /**
     * Valida o token, atualiza a senha do usuário e invalida o token.
     *
     * @return {@code true} em caso de sucesso, {@code false} se o token for inválido.
     */
    @Transactional
    public boolean resetarSenha(String valorToken, String novaSenha) {
        Optional<PasswordResetToken> optToken = tokenRepository.findByToken(valorToken)
            .filter(PasswordResetToken::isValido);

        if (optToken.isEmpty()) {
            log.warn("[RESET] Token inválido ou expirado: {}", valorToken);
            return false;
        }

        PasswordResetToken token = optToken.get();
        Usuario usuario = token.getUsuario();

        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);

        token.setUsado(true);
        tokenRepository.save(token);

        log.info("[RESET] Senha redefinida para o usuário: {}", usuario.getLogin());
        return true;
    }

    // -----------------------------------------------------------------------
    // Envio de e-mail
    // -----------------------------------------------------------------------

    private void enviarEmailReset(Usuario usuario, String valorToken) {
        String link = baseUrl + "/auth/reset-password/" + valorToken;
        String corpo = buildCorpoEmail(usuario.getNome(), link);

        try {
            var msg = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(remetente);
            helper.setTo(usuario.getEmail());
            helper.setSubject("Redefinição de Senha — Sistema Páscoa");
            helper.setText(corpo, true);
            mailSender.send(msg);
            log.info("[RESET] E-mail enviado para {} (login: {})", usuario.getEmail(), usuario.getLogin());
        } catch (MessagingException | RuntimeException e) {
            log.error("[RESET] Falha ao enviar e-mail para {}: {}", usuario.getEmail(), e.getMessage());
            // Não propaga — o token já foi persistido; admin pode reenviar manualmente.
        }
    }

    private String buildCorpoEmail(String nome, String link) {
        return """
            <div style="font-family:Arial,sans-serif;max-width:480px;margin:0 auto;padding:24px;">
              <div style="text-align:center;margin-bottom:24px;">
                <span style="font-size:2.5rem;">🥚</span>
                <h2 style="color:#198754;margin:8px 0 4px;">Páscoa Gestão</h2>
                <p style="color:#6c757d;margin:0;font-size:13px;">Sistema de Gestão de Ovos Artesanais</p>
              </div>
              <h3 style="color:#212529;">Redefinição de Senha</h3>
              <p>Olá, <strong>%s</strong>!</p>
              <p>Recebemos uma solicitação para redefinir a senha da sua conta no <strong>Sistema Páscoa</strong>.</p>
              <p>Clique no botão abaixo para criar uma nova senha:</p>
              <div style="text-align:center;margin:28px 0;">
                <a href="%s"
                   style="background:#198754;color:#fff;padding:12px 28px;text-decoration:none;
                          border-radius:6px;font-weight:bold;display:inline-block;">
                  Redefinir Minha Senha
                </a>
              </div>
              <p style="color:#6c757d;font-size:12px;">
                <strong>Este link expira em %d minutos.</strong><br>
                Se você não solicitou a redefinição de senha, ignore este e-mail — sua conta está segura.
              </p>
              <hr style="border:none;border-top:1px solid #dee2e6;margin:20px 0;">
              <p style="color:#6c757d;font-size:11px;">
                Ou copie e cole este link no navegador:<br>
                <a href="%s" style="color:#198754;word-break:break-all;">%s</a>
              </p>
            </div>
            """.formatted(nome, link, EXPIRACAO_MINUTOS, link, link);
    }
}
