package br.com.seuprojeto.pascoa.orcamento.controller;

import br.com.seuprojeto.pascoa.cadastro.repository.ClienteRepository;
import br.com.seuprojeto.pascoa.cadastro.repository.ProdutoRepository;
import br.com.seuprojeto.pascoa.orcamento.dto.OrcamentoForm;
import br.com.seuprojeto.pascoa.orcamento.entity.Orcamento;
import br.com.seuprojeto.pascoa.orcamento.service.OrcamentoService;
import br.com.seuprojeto.pascoa.pedido.entity.Pedido;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/orcamentos")
@RequiredArgsConstructor
public class OrcamentoController {

    private final OrcamentoService service;
    private final ClienteRepository clienteRepo;
    private final ProdutoRepository produtoRepo;

    // ── Lista ──────────────────────────────────────────────────────────────

    @GetMapping
    public String lista(Model model) {
        model.addAttribute("orcamentos", service.listar());
        return "orcamentos/lista";
    }

    // ── Detalhe ────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Model model, HttpServletRequest request) {
        Orcamento orc = service.buscarPorId(id);
        model.addAttribute("orc", orc);
        if (orc.isPendente()) {
            String baseUrl = request.getScheme() + "://" + request.getServerName()
                    + (request.getServerPort() != 80 && request.getServerPort() != 443
                        ? ":" + request.getServerPort() : "");
            model.addAttribute("linkAprovacao", baseUrl + "/orcamento-publico/" + orc.getTokenAprovacao());
        }
        return "orcamentos/detalhe";
    }

    // ── Novo ───────────────────────────────────────────────────────────────

    @GetMapping("/novo")
    public String novoForm(Model model) {
        model.addAttribute("form", new OrcamentoForm());
        model.addAttribute("clientes", clienteRepo.findAll());
        model.addAttribute("produtos", produtoRepo.findByAtivoTrueOrderByNomeAsc());
        return "orcamentos/form";
    }

    @PostMapping("/novo")
    public String novo(
            @Valid @ModelAttribute("form") OrcamentoForm form,
            BindingResult result,
            @AuthenticationPrincipal UserDetails usuario,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (result.hasErrors() || form.getItens().isEmpty()) {
            if (form.getItens().isEmpty()) {
                model.addAttribute("erroItens", "Adicione pelo menos um item ao orçamento.");
            }
            model.addAttribute("clientes", clienteRepo.findAll());
            model.addAttribute("produtos", produtoRepo.findByAtivoTrueOrderByNomeAsc());
            return "orcamentos/form";
        }
        Orcamento orc = service.criar(form, usuario.getUsername());
        redirectAttributes.addFlashAttribute("sucesso", "Orçamento #" + orc.getId() + " criado com sucesso.");
        return "redirect:/orcamentos/" + orc.getId();
    }

    // ── Editar ─────────────────────────────────────────────────────────────

    @GetMapping("/{id}/editar")
    public String editarForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Orcamento orc = service.buscarPorId(id);
        if (!orc.isPendente()) {
            redirectAttributes.addFlashAttribute("erro", "Apenas orçamentos pendentes podem ser editados.");
            return "redirect:/orcamentos/" + id;
        }
        OrcamentoForm form = new OrcamentoForm();
        form.setClienteId(orc.getCliente().getId());
        form.setValidade(orc.getValidade());
        form.setObservacoes(orc.getObservacoes());

        model.addAttribute("form", form);
        model.addAttribute("orcId", id);
        model.addAttribute("clientes", clienteRepo.findAll());
        model.addAttribute("produtos", produtoRepo.findByAtivoTrueOrderByNomeAsc());
        model.addAttribute("itensExistentes", orc.getItens());
        return "orcamentos/form";
    }

    @PostMapping("/{id}/editar")
    public String editar(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") OrcamentoForm form,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (result.hasErrors() || form.getItens().isEmpty()) {
            if (form.getItens().isEmpty()) {
                model.addAttribute("erroItens", "Adicione pelo menos um item ao orçamento.");
            }
            model.addAttribute("orcId", id);
            model.addAttribute("clientes", clienteRepo.findAll());
            model.addAttribute("produtos", produtoRepo.findByAtivoTrueOrderByNomeAsc());
            return "orcamentos/form";
        }
        service.atualizar(id, form);
        redirectAttributes.addFlashAttribute("sucesso", "Orçamento atualizado.");
        return "redirect:/orcamentos/" + id;
    }

    // ── Excluir ────────────────────────────────────────────────────────────

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            service.excluir(id);
            redirectAttributes.addFlashAttribute("sucesso", "Orçamento excluído.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/orcamentos";
    }

    // ── PDF ────────────────────────────────────────────────────────────────

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        byte[] bytes = service.gerarPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"orcamento_" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    // ── Converter em Pedido ────────────────────────────────────────────────

    @PostMapping("/{id}/converter")
    public String converter(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Pedido pedido = service.converter(id);
            redirectAttributes.addFlashAttribute("sucesso",
                    "Orçamento convertido! Pedido #" + pedido.getId() + " criado.");
            return "redirect:/pedidos/" + pedido.getId();
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
            return "redirect:/orcamentos/" + id;
        }
    }
}
