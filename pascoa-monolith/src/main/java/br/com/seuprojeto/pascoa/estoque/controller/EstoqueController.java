package br.com.seuprojeto.pascoa.estoque.controller;

import br.com.seuprojeto.pascoa.cadastro.service.MateriaPrimaService;
import br.com.seuprojeto.pascoa.estoque.dto.AjusteEstoqueForm;
import br.com.seuprojeto.pascoa.estoque.dto.EntradaEstoqueForm;
import br.com.seuprojeto.pascoa.estoque.entity.TipoMovimentacao;
import br.com.seuprojeto.pascoa.estoque.service.EstoqueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/estoque")
@RequiredArgsConstructor
public class EstoqueController {

    private final EstoqueService estoqueService;
    private final MateriaPrimaService materiaPrimaService;

    // -----------------------------------------------------------------------
    // Histórico de movimentações
    // -----------------------------------------------------------------------

    @GetMapping("/movimentacoes")
    public String movimentacoes(@RequestParam(required = false) Long mpId,
                                @RequestParam(required = false) TipoMovimentacao tipo,
                                Model model) {
        model.addAttribute("movimentacoes", estoqueService.filtrar(mpId, tipo));
        model.addAttribute("materiasPrimas", materiaPrimaService.listarTodas());
        model.addAttribute("tipos", TipoMovimentacao.values());
        model.addAttribute("mpIdFiltro", mpId);
        model.addAttribute("tipoFiltro", tipo);
        return "estoque/movimentacoes";
    }

    // -----------------------------------------------------------------------
    // Entrada de matéria-prima
    // -----------------------------------------------------------------------

    @GetMapping("/entrada")
    public String entradaForm(@RequestParam(required = false) Long mpId, Model model) {
        EntradaEstoqueForm form = new EntradaEstoqueForm();
        if (mpId != null) form.setMateriaPrimaId(mpId);
        model.addAttribute("form", form);
        model.addAttribute("materiasPrimas", materiaPrimaService.listarTodas());
        return "estoque/entrada";
    }

    @PostMapping("/entrada/salvar")
    public String entradaSalvar(@Valid @ModelAttribute("form") EntradaEstoqueForm form,
                                BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("materiasPrimas", materiaPrimaService.listarTodas());
            return "estoque/entrada";
        }
        try {
            estoqueService.registrarEntrada(form);
            ra.addFlashAttribute("sucesso", "Entrada registrada com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao registrar entrada: " + e.getMessage());
        }
        return "redirect:/estoque/movimentacoes";
    }

    // -----------------------------------------------------------------------
    // Ajuste de inventário
    // -----------------------------------------------------------------------

    @GetMapping("/ajuste")
    public String ajusteForm(@RequestParam(required = false) Long mpId, Model model) {
        AjusteEstoqueForm form = new AjusteEstoqueForm();
        if (mpId != null) form.setMateriaPrimaId(mpId);
        model.addAttribute("form", form);
        model.addAttribute("materiasPrimas", materiaPrimaService.listarTodas());
        return "estoque/ajuste";
    }

    @PostMapping("/ajuste/salvar")
    public String ajusteSalvar(@Valid @ModelAttribute("form") AjusteEstoqueForm form,
                               BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("materiasPrimas", materiaPrimaService.listarTodas());
            return "estoque/ajuste";
        }
        try {
            estoqueService.registrarAjuste(form);
            ra.addFlashAttribute("sucesso", "Ajuste de inventário registrado!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao registrar ajuste: " + e.getMessage());
        }
        return "redirect:/estoque/movimentacoes";
    }
}
