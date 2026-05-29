package br.com.seuprojeto.pascoa.seguranca.service;

import br.com.seuprojeto.pascoa.seguranca.entity.Role;
import br.com.seuprojeto.pascoa.seguranca.entity.Usuario;
import br.com.seuprojeto.pascoa.seguranca.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class TwoFactorAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    public static final String PENDING_2FA_AUTH    = "PENDING_2FA_AUTH";
    public static final String PENDING_2FA_USER_ID = "PENDING_2FA_USER_ID";

    private final UsuarioRepository usuarioRepository;
    private final TotpService totpService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        boolean isAdmin = authentication.getAuthorities().contains(
            new SimpleGrantedAuthority("ROLE_" + Role.ADMIN.name()));

        if (!isAdmin) {
            response.sendRedirect("/dashboard");
            return;
        }

        Usuario usuario = usuarioRepository.findByLogin(authentication.getName()).orElse(null);
        if (usuario == null) {
            response.sendRedirect("/dashboard");
            return;
        }

        // Salva autenticação pendente e remove do SecurityContext da sessão
        HttpSession session = request.getSession();
        session.setAttribute(PENDING_2FA_AUTH, authentication);
        session.setAttribute(PENDING_2FA_USER_ID, usuario.getId());
        session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);

        String destino = usuario.isTotpAtivado() ? "/2fa/verificar" : "/2fa/setup";
        response.sendRedirect(destino);
    }
}
