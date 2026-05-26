package br.com.seuprojeto.pascoa.qualidade.controller;

import br.com.seuprojeto.pascoa.cadastro.repository.ProdutoRepository;
import br.com.seuprojeto.pascoa.producao.entity.OrdemProducao;
import br.com.seuprojeto.pascoa.qualidade.entity.InspecaoQualidade;
import br.com.seuprojeto.pascoa.qualidade.service.QualidadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/qualidade")
@RequiredArgsConstructor
public class QualidadeController {

    private final QualidadeService service;
    private final ProdutoRepository produtoRepo;

    // ── Dashboard ─────────────────────────────────────────────────────────

    @GetMapping
    public String lista(Model model) {
        model.addAttribute("inspecoes", service.listarInspecoes());
        model.addAttribute("totalAprovadas", service.totalAprovadas());
        model.addAttribute("totalReprovadas", service.totalReprovadas());
        model.addAttribute("produtos", produtoRepo.findByAtivoTrueOrderByNomeAsc());
        return "qualidade/lista";
    }

    // ── Checklist ─────────────────────────────────────────────────────────

    @GetMapping("/checklist/{produtoId}")
    public String checklist(@PathVariable Long produtoId, Model model) {
        model.addAttribute("produto", produtoRepo.findById(produtoId)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado")));
        model.addAttribute("itens", service.listarChecklist(produtoId));
        return "qualidade/checklist";
    }

    @PostMapping("/checklist/{produtoId}")
    public String adicionarItem(@PathVariable Long produtoId,
                                @RequestParam String descricao,
                                RedirectAttributes ra) {
        if (descricao == null || descricao.isBlank()) {
            ra.addFlashAttribute("erro", "Descrição não pode estar vazia.");
        } else {
            service.adicionarItem(produtoId, descricao);
            ra.addFlashAttribute("sucesso", "Item adicionado ao checklist.");
        }
        return "redirect:/qualidade/checklist/" + produtoId;
    }

    @PostMapping("/checklist/item/{id}/excluir")
    public String excluirItem(@PathVariable Long id,
                              @RequestParam Long produtoId,
                              RedirectAttributes ra) {
        service.excluirItem(id);
        ra.addFlashAttribute("sucesso", "Item removido.");
        return "redirect:/qualidade/checklist/" + produtoId;
    }

    @PostMapping("/checklist/item/{id}/toggle")
    public String toggleItem(@PathVariable Long id,
                             @RequestParam Long produtoId,
                             RedirectAttributes ra) {
        service.toggleAtivo(id);
        return "redirect:/qualidade/checklist/" + produtoId;
    }

    // ── Inspeção ──────────────────────────────────────────────────────────

    @GetMapping("/inspecao/nova/{ordemId}")
    public String novaInspecaoForm(@PathVariable Long ordemId, Model model,
                                   RedirectAttributes ra) {
        // Redireciona para detalhe se já existe inspeção
        Optional<InspecaoQualidade> existente = service.buscarInspecaoPorOrdem(ordemId);
        if (existente.isPresent()) {
            ra.addFlashAttribute("erro", "Esta ordem já foi inspecionada.");
            return "redirect:/qualidade/inspecao/" + existente.get().getId();
        }

        OrdemProducao ordem = service.buscarOrdem(ordemId);
        model.addAttribute("ordem", ordem);
        model.addAttribute("checklistItens", service.listarChecklistAtivo(ordem.getProduto().getId()));
        return "qualidade/inspecao-form";
    }

    @PostMapping("/inspecao/nova/{ordemId}")
    public String salvarInspecao(@PathVariable Long ordemId,
                                 @RequestParam String inspetor,
                                 @RequestParam boolean aprovado,
                                 @RequestParam(required = false) String observacoes,
                                 @RequestParam(required = false) List<Long> itemsVerificados,
                                 @AuthenticationPrincipal UserDetails usuario,
                                 RedirectAttributes ra) {
        // Se inspetor não preenchido, usa login
        String nomeInspetor = (inspetor != null && !inspetor.isBlank())
                ? inspetor : usuario.getUsername();
        try {
            InspecaoQualidade inspecao = service.registrarInspecao(
                    ordemId, nomeInspetor, aprovado, observacoes, itemsVerificados);
            ra.addFlashAttribute("sucesso", "Inspeção registrada com sucesso.");
            return "redirect:/qualidade/inspecao/" + inspecao.getId();
        } catch (Exception e) {
            ra.addFlashAttribute("erro", e.getMessage());
            return "redirect:/qualidade/inspecao/nova/" + ordemId;
        }
    }

    @GetMapping("/inspecao/{id}")
    public String detalheInspecao(@PathVariable Long id, Model model) {
        model.addAttribute("inspecao", service.buscarInspecao(id));
        return "qualidade/inspecao-detalhe";
    }
}
