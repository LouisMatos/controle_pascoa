package br.com.seuprojeto.pascoa.seguranca.controller;

import br.com.seuprojeto.pascoa.seguranca.entity.Usuario;
import br.com.seuprojeto.pascoa.seguranca.repository.UsuarioRepository;
import br.com.seuprojeto.pascoa.seguranca.service.TotpService;
import br.com.seuprojeto.pascoa.seguranca.service.TwoFactorAuthenticationSuccessHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/2fa")
@RequiredArgsConstructor
public class TwoFactorController {

    private static final int MAX_TENTATIVAS = 5;

    private final TotpService totpService;
    private final UsuarioRepository usuarioRepository;

    // ── Setup inicial (primeiro login ADMIN) ─────────────────────────────────

    @GetMapping("/setup")
    public String setupForm(HttpSession session, Model model) {
        Usuario usuario = usuarioPendente(session);
        if (usuario == null) return "redirect:/login";
        if (usuario.isTotpAtivado()) return "redirect:/2fa/verificar";

        if (usuario.getTotpSecret() == null) {
            usuario.setTotpSecret(totpService.gerarSegredo());
            usuarioRepository.save(usuario);
        }

        model.addAttribute("qrCode", totpService.qrCodeDataUri(usuario.getTotpSecret(), usuario.getLogin()));
        model.addAttribute("segredo", usuario.getTotpSecret());
        return "2fa/setup";
    }

    @PostMapping("/setup")
    public String setupConfirmar(@RequestParam String codigo,
                                 HttpSession session,
                                 HttpServletRequest request,
                                 HttpServletResponse response,
                                 RedirectAttributes ra) throws Exception {
        Usuario usuario = usuarioPendente(session);
        if (usuario == null) return "redirect:/login";

        int codigoInt = parseCodigoOuErro(codigo, ra, "redirect:/2fa/setup");
        if (codigoInt < 0) return "redirect:/2fa/setup";

        if (!totpService.validar(usuario.getTotpSecret(), codigoInt)) {
            ra.addFlashAttribute("erro", "Código inválido. Verifique o horário do seu dispositivo e tente novamente.");
            return "redirect:/2fa/setup";
        }

        usuario.setTotpAtivado(true);
        usuario.setTentativasTotpFalhas(0);
        usuarioRepository.save(usuario);

        return completarAutenticacao(session, request, response);
    }

    // ── Verificação (logins subsequentes) ────────────────────────────────────

    @GetMapping("/verificar")
    public String verificarForm(HttpSession session) {
        if (usuarioPendente(session) == null) return "redirect:/login";
        return "2fa/verificar";
    }

    @PostMapping("/verificar")
    public String verificar(@RequestParam String codigo,
                            HttpSession session,
                            HttpServletRequest request,
                            HttpServletResponse response,
                            RedirectAttributes ra) throws Exception {
        Usuario usuario = usuarioPendente(session);
        if (usuario == null) return "redirect:/login";

        if (usuario.getTentativasTotpFalhas() >= MAX_TENTATIVAS) {
            ra.addFlashAttribute("erro", "Conta bloqueada por excesso de tentativas. Contate o administrador.");
            return "redirect:/2fa/verificar";
        }

        int codigoInt = parseCodigoOuErro(codigo, ra, "redirect:/2fa/verificar");
        if (codigoInt < 0) return "redirect:/2fa/verificar";

        if (!totpService.validar(usuario.getTotpSecret(), codigoInt)) {
            usuario.setTentativasTotpFalhas(usuario.getTentativasTotpFalhas() + 1);
            usuarioRepository.save(usuario);
            int restantes = MAX_TENTATIVAS - usuario.getTentativasTotpFalhas();
            ra.addFlashAttribute("erro", "Código inválido. Tentativas restantes: " + restantes + ".");
            return "redirect:/2fa/verificar";
        }

        usuario.setTentativasTotpFalhas(0);
        usuarioRepository.save(usuario);
        return completarAutenticacao(session, request, response);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Usuario usuarioPendente(HttpSession session) {
        Long userId = (Long) session.getAttribute(TwoFactorAuthenticationSuccessHandler.PENDING_2FA_USER_ID);
        if (userId == null) return null;
        return usuarioRepository.findById(userId).orElse(null);
    }

    private String completarAutenticacao(HttpSession session,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        Authentication auth = (Authentication) session.getAttribute(
            TwoFactorAuthenticationSuccessHandler.PENDING_2FA_AUTH);
        if (auth == null) return "redirect:/login";

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        session.removeAttribute(TwoFactorAuthenticationSuccessHandler.PENDING_2FA_AUTH);
        session.removeAttribute(TwoFactorAuthenticationSuccessHandler.PENDING_2FA_USER_ID);
        return "redirect:/dashboard";
    }

    private int parseCodigoOuErro(String codigo, RedirectAttributes ra, String redirect) {
        try {
            String limpo = codigo.replaceAll("\\s", "");
            return Integer.parseInt(limpo);
        } catch (NumberFormatException e) {
            ra.addFlashAttribute("erro", "Código inválido. Digite apenas os 6 dígitos.");
            return -1;
        }
    }
}
