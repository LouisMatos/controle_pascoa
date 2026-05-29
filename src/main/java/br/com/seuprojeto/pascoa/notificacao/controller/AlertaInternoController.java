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

    /** Página completa com todos os alertas, com suporte a filtro. */
    @GetMapping
    public String lista(@RequestParam(required = false, defaultValue = "todos") String filtro,
                        Model model) {
        boolean soNaoLidos = "naoLidos".equals(filtro);
        model.addAttribute("alertas",  soNaoLidos ? service.naoLidos() : service.todos());
        model.addAttribute("naoLidos", service.contarNaoLidos());
        model.addAttribute("filtro",   filtro);
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

    /** Exclui um alerta individual. */
    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id,
                          @RequestParam(required = false, defaultValue = "todos") String filtro,
                          RedirectAttributes ra) {
        service.excluir(id);
        ra.addFlashAttribute("sucesso", "Alerta removido.");
        return "redirect:/alertas?filtro=" + filtro;
    }

    /** Exclui em lote todos os alertas já lidos. */
    @PostMapping("/excluir-lidos")
    public String excluirLidos(RedirectAttributes ra) {
        int removidos = service.excluirLidas();
        ra.addFlashAttribute("sucesso", removidos + " alerta(s) lido(s) removido(s).");
        return "redirect:/alertas";
    }
}
