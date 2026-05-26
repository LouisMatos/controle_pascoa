package br.com.seuprojeto.pascoa.orcamento.controller;

import br.com.seuprojeto.pascoa.orcamento.entity.Orcamento;
import br.com.seuprojeto.pascoa.orcamento.service.OrcamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/orcamento-publico")
@RequiredArgsConstructor
public class OrcamentoPublicoController {

    private final OrcamentoService service;

    @GetMapping("/{token}")
    public String pagina(@PathVariable String token, Model model) {
        try {
            Orcamento orc = service.buscarPorToken(token);
            model.addAttribute("orc", orc);
        } catch (Exception e) {
            model.addAttribute("erro", "Orçamento não encontrado ou link inválido.");
        }
        return "orcamentos/aprovacao";
    }

    @PostMapping("/{token}/aprovar")
    public String aprovar(@PathVariable String token, RedirectAttributes redirectAttributes) {
        try {
            service.aprovar(token);
            redirectAttributes.addFlashAttribute("sucesso", "Orçamento aprovado! Entraremos em contato em breve.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/orcamento-publico/" + token;
    }

    @PostMapping("/{token}/recusar")
    public String recusar(@PathVariable String token, RedirectAttributes redirectAttributes) {
        try {
            service.recusar(token);
            redirectAttributes.addFlashAttribute("sucesso", "Orçamento recusado. Obrigado pelo retorno.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/orcamento-publico/" + token;
    }
}
