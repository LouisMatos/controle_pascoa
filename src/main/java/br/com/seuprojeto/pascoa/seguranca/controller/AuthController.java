package br.com.seuprojeto.pascoa.seguranca.controller;

import br.com.seuprojeto.pascoa.seguranca.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Endpoints públicos de autenticação complementares ao Spring Security:
 * <ul>
 *   <li>GET  /auth/forgot-password  — exibe formulário de recuperação de senha</li>
 *   <li>POST /auth/forgot-password  — solicita envio do link por e-mail</li>
 *   <li>GET  /auth/reset-password/{token} — exibe formulário de nova senha</li>
 *   <li>POST /auth/reset-password/{token} — aplica a nova senha</li>
 * </ul>
 * Todos os endpoints são públicos (permitAll no SecurityConfig).
 */
@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final PasswordResetService passwordResetService;

    // ── Passo 1: formulário de e-mail / login ────────────────────────────────

    @GetMapping("/forgot-password")
    public String forgotPasswordForm() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam String loginOuEmail,
                                 RedirectAttributes ra) {
        // Executa sempre — nunca revelamos se o usuário existe (anti-enumeração)
        passwordResetService.solicitarReset(loginOuEmail);
        ra.addFlashAttribute("sucesso",
            "Se o usuário existir e tiver e-mail cadastrado, um link foi enviado. "
            + "Verifique sua caixa de entrada (e o spam).");
        return "redirect:/auth/forgot-password";
    }

    // ── Passo 2: formulário de nova senha ────────────────────────────────────

    @GetMapping("/reset-password/{token}")
    public String resetPasswordForm(@PathVariable String token,
                                    Model model,
                                    RedirectAttributes ra) {
        if (passwordResetService.validarToken(token).isEmpty()) {
            ra.addFlashAttribute("erro",
                "Link inválido ou expirado. Solicite um novo link de recuperação.");
            return "redirect:/auth/forgot-password";
        }
        model.addAttribute("token", token);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password/{token}")
    public String resetPassword(@PathVariable String token,
                                @RequestParam String novaSenha,
                                @RequestParam String confirmarSenha,
                                RedirectAttributes ra) {
        if (!novaSenha.equals(confirmarSenha)) {
            ra.addFlashAttribute("erro", "As senhas não coincidem. Tente novamente.");
            return "redirect:/auth/reset-password/" + token;
        }
        if (novaSenha.length() < 8) {
            ra.addFlashAttribute("erro", "A senha deve ter no mínimo 8 caracteres.");
            return "redirect:/auth/reset-password/" + token;
        }

        boolean ok = passwordResetService.resetarSenha(token, novaSenha);
        if (!ok) {
            ra.addFlashAttribute("erro",
                "Link inválido ou expirado. Solicite um novo link de recuperação.");
            return "redirect:/auth/forgot-password";
        }

        ra.addFlashAttribute("sucesso",
            "Senha redefinida com sucesso! Faça login com sua nova senha.");
        return "redirect:/login";
    }
}
