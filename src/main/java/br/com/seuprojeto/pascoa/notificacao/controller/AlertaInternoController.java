package br.com.seuprojeto.pascoa.notificacao.controller;

import br.com.seuprojeto.pascoa.notificacao.service.AlertaInternoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/alertas")
@RequiredArgsConstructor
public class AlertaInternoController {

    private final AlertaInternoService service;

    /** Página completa com todos os alertas. */
    @GetMapping
    public String lista(Model model) {
        model.addAttribute("alertas", service.todos());
        model.addAttribute("naoLidos", service.contarNaoLidos());
        return "notificacoes/alertas";
    }

    /** Marca um alerta específico como lido e redireciona para seu link. */
    @PostMapping("/{id}/lido")
    public String marcarLido(@PathVariable Long id,
                             @RequestParam(required = false) String redirect,
                             RedirectAttributes ra) {
        service.marcarLido(id);
        if (redirect != null && !redirect.isBlank()) {
            return "redirect:" + redirect;
        }
        return "redirect:/alertas";
    }

    /** Marca todos como lidos. */
    @PostMapping("/marcar-todos-lidos")
    public String marcarTodosLidos(RedirectAttributes ra) {
        service.marcarTodasLidas();
        ra.addFlashAttribute("sucesso", "Todos os alertas foram marcados como lidos.");
        return "redirect:/alertas";
    }
}
