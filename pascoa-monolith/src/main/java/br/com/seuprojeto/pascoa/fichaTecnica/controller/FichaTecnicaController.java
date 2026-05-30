package br.com.seuprojeto.pascoa.fichaTecnica.controller;

import br.com.seuprojeto.pascoa.cadastro.entity.Unidade;
import br.com.seuprojeto.pascoa.cadastro.service.MateriaPrimaService;
import br.com.seuprojeto.pascoa.cadastro.service.ProdutoService;
import br.com.seuprojeto.pascoa.fichaTecnica.dto.ItemFichaForm;
import br.com.seuprojeto.pascoa.fichaTecnica.entity.FichaTecnica;
import br.com.seuprojeto.pascoa.fichaTecnica.service.FichaTecnicaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/fichas")
@RequiredArgsConstructor
public class FichaTecnicaController {

    private final FichaTecnicaService fichaService;
    private final ProdutoService produtoService;
    private final MateriaPrimaService materiaPrimaService;

    @GetMapping("/{produtoId}")
    public String detalhe(@PathVariable Long produtoId, Model model) {
        var produto = produtoService.buscarPorId(produtoId);
        FichaTecnica ficha = fichaService.buscarOuCriar(produtoId);
        // recarrega com itens via JOIN FETCH
        ficha = fichaService.buscarPorProduto(produtoId);

        BigDecimal custoTotal = fichaService.calcularCustoTotal(ficha);
        BigDecimal custoPorUnidade = fichaService.calcularCustoPorUnidade(ficha);
        BigDecimal margem = fichaService.calcularMargemLucro(produto.getPrecoVenda(), custoPorUnidade);

        model.addAttribute("produto", produto);
        model.addAttribute("ficha", ficha);
        model.addAttribute("unidades", Unidade.values());
        model.addAttribute("materiasPrimas", materiaPrimaService.listarTodas());
        model.addAttribute("itemForm", new ItemFichaForm());
        model.addAttribute("custoTotal", custoTotal);
        model.addAttribute("custoPorUnidade", custoPorUnidade);
        model.addAttribute("margem", margem);
        return "fichas/detalhe";
    }

    @PostMapping("/{produtoId}/salvar")
    public String salvarInfo(@PathVariable Long produtoId,
                             @RequestParam BigDecimal rendimento,
                             @RequestParam Unidade unidadeRendimento,
                             @RequestParam(required = false) String observacoes,
                             RedirectAttributes ra) {
        try {
            fichaService.salvarInfo(produtoId, rendimento, unidadeRendimento, observacoes);
            ra.addFlashAttribute("sucesso", "Informações da ficha técnica salvas!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao salvar: " + e.getMessage());
        }
        return "redirect:/fichas/" + produtoId;
    }

    @PostMapping("/{produtoId}/item")
    public String adicionarItem(@PathVariable Long produtoId,
                                @Valid @ModelAttribute("itemForm") ItemFichaForm form,
                                BindingResult result,
                                RedirectAttributes ra) {
        if (result.hasErrors()) {
            ra.addFlashAttribute("erro", "Verifique os campos: matéria-prima e quantidade são obrigatórios.");
            return "redirect:/fichas/" + produtoId;
        }
        try {
            fichaService.adicionarItem(produtoId, form.getMateriaPrimaId(), form.getQuantidade());
            ra.addFlashAttribute("sucesso", "Ingrediente adicionado com sucesso!");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao adicionar ingrediente: " + e.getMessage());
        }
        return "redirect:/fichas/" + produtoId;
    }

    @PostMapping("/{produtoId}/item/{itemId}/excluir")
    public String removerItem(@PathVariable Long produtoId,
                              @PathVariable Long itemId,
                              RedirectAttributes ra) {
        try {
            fichaService.removerItem(itemId);
            ra.addFlashAttribute("sucesso", "Ingrediente removido.");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao remover: " + e.getMessage());
        }
        return "redirect:/fichas/" + produtoId;
    }
}
