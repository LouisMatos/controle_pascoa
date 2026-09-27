package br.com.seuprojeto.pascoa.config;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SistemaController {

    private final ConfiguracaoSistemaRepository sistemaRepository;

    // ── Página de configurações do sistema ────────────────────────────────────

    @GetMapping("/admin/sistema")
    public String pagina(Model model) {
        ConfiguracaoSistema config = sistemaRepository.findById(1L)
            .orElseGet(this::criarPadrao);
        model.addAttribute("config", config);
        return "admin/sistema";
    }

    @PostMapping("/admin/sistema")
    public String salvar(@ModelAttribute ConfiguracaoSistema config,
                         RedirectAttributes ra) {
        config.setId(1L); // garante singleton
        sistemaRepository.save(config);
        ra.addFlashAttribute("sucesso", "Configurações do sistema salvas com sucesso.");
        return "redirect:/admin/sistema";
    }

    // ── Toggle rápido de manutenção ───────────────────────────────────────────

    @PostMapping("/admin/sistema/manutencao/toggle")
    public String toggleManutencao(RedirectAttributes ra) {
        ConfiguracaoSistema config = sistemaRepository.findById(1L)
            .orElseGet(this::criarPadrao);
        boolean novoEstado = !Boolean.TRUE.equals(config.getModoManutencao());
        config.setModoManutencao(novoEstado);
        sistemaRepository.save(config);
        ra.addFlashAttribute("sucesso",
            novoEstado ? "⚠️ Modo manutenção ATIVADO. Usuários não-ADMIN serão bloqueados."
                       : "✅ Modo manutenção DESATIVADO. Sistema acessível normalmente.");
        return "redirect:/admin/sistema";
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private ConfiguracaoSistema criarPadrao() {
        return sistemaRepository.save(new ConfiguracaoSistema());
    }
}
