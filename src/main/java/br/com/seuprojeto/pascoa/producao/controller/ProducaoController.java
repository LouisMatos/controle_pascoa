package br.com.seuprojeto.pascoa.producao.controller;

import br.com.seuprojeto.pascoa.producao.entity.OrdemProducao;
import br.com.seuprojeto.pascoa.producao.entity.StatusOrdem;
import br.com.seuprojeto.pascoa.producao.service.ProducaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/producao")
@RequiredArgsConstructor
public class ProducaoController {

    private final ProducaoService producaoService;

    @GetMapping
    public String fila(@RequestParam(required = false) StatusOrdem status, Model model) {
        List<OrdemProducao> ordens = status != null
            ? producaoService.listarPorStatus(status)
            : producaoService.listarTodas();
        model.addAttribute("ordens", ordens);
        model.addAttribute("statusFiltro", status);
        model.addAttribute("statusList", StatusOrdem.values());
        return "producao/fila";
    }

    @GetMapping("/kanban")
    public String kanban(Model model) {
        var mapa = producaoService.listarKanban();
        model.addAttribute("pendentes",    mapa.get(StatusOrdem.PENDENTE));
        model.addAttribute("emAndamento",  mapa.get(StatusOrdem.EM_ANDAMENTO));
        model.addAttribute("concluidas",   mapa.get(StatusOrdem.CONCLUIDA));
        model.addAttribute("canceladas",   mapa.get(StatusOrdem.CANCELADA));
        return "producao/kanban";
    }

    /** Ação rápida pelo Kanban — redireciona de volta para o board. */
    @PostMapping("/{id}/iniciar-kanban")
    public String iniciarKanban(@PathVariable Long id, RedirectAttributes ra) {
        try {
            producaoService.iniciarProducao(id);
        } catch (Exception e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/producao/kanban";
    }

    @PostMapping("/{id}/concluir-kanban")
    public String concluirKanban(@PathVariable Long id, RedirectAttributes ra) {
        try {
            producaoService.concluirOrdem(id);
        } catch (Exception e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/producao/kanban";
    }

    @PostMapping("/{id}/cancelar-kanban")
    public String cancelarKanban(@PathVariable Long id, RedirectAttributes ra) {
        try {
            producaoService.cancelarOrdem(id);
        } catch (Exception e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/producao/kanban";
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Model model) {
        OrdemProducao ordem = producaoService.buscarPorId(id);
        model.addAttribute("ordem", ordem);
        producaoService.buscarFicha(ordem.getProduto().getId())
            .ifPresent(f -> model.addAttribute("ficha", f));
        return "producao/detalhe";
    }

    @PostMapping("/{id}/iniciar")
    public String iniciar(@PathVariable Long id, RedirectAttributes ra) {
        try {
            producaoService.iniciarProducao(id);
            ra.addFlashAttribute("sucesso", "Produção iniciada!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/producao/" + id;
    }

    @PostMapping("/{id}/concluir")
    public String concluir(@PathVariable Long id, RedirectAttributes ra) {
        try {
            producaoService.concluirOrdem(id);
            ra.addFlashAttribute("sucesso", "Ordem concluída! Estoque atualizado automaticamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/producao/" + id;
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable Long id, RedirectAttributes ra) {
        try {
            producaoService.cancelarOrdem(id);
            ra.addFlashAttribute("sucesso", "Ordem cancelada.");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/producao";
    }
}
