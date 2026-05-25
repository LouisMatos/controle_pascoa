package br.com.seuprojeto.pascoa.pedido.controller;

import br.com.seuprojeto.pascoa.cadastro.service.ClienteService;
import br.com.seuprojeto.pascoa.cadastro.service.ProdutoService;
import br.com.seuprojeto.pascoa.pedido.dto.ItemPedidoForm;
import br.com.seuprojeto.pascoa.pedido.dto.PagamentoForm;
import br.com.seuprojeto.pascoa.pedido.dto.PedidoForm;
import br.com.seuprojeto.pascoa.pedido.entity.Pedido;
import br.com.seuprojeto.pascoa.pedido.entity.StatusPedido;
import br.com.seuprojeto.pascoa.pedido.entity.TipoPagamento;
import br.com.seuprojeto.pascoa.pedido.service.PedidoService;
import br.com.seuprojeto.pascoa.producao.service.ProducaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequestMapping("/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;
    private final ClienteService clienteService;
    private final ProdutoService produtoService;
    private final ProducaoService producaoService;

    // -----------------------------------------------------------------------
    // Lista
    // -----------------------------------------------------------------------

    @GetMapping
    public String listar(@RequestParam(required = false) StatusPedido status, Model model) {
        var pedidos = (status != null)
            ? pedidoService.listarPorStatus(status)
            : pedidoService.listarTodos();
        model.addAttribute("pedidos", pedidos);
        model.addAttribute("statusFiltro", status);
        model.addAttribute("statusValues", StatusPedido.values());
        return "pedidos/lista";
    }

    // -----------------------------------------------------------------------
    // Wizard de criação de pedido (Item 6)
    // -----------------------------------------------------------------------

    @GetMapping("/wizard")
    public String wizard(Model model) {
        model.addAttribute("clientes", clienteService.listarTodos());
        model.addAttribute("produtos", produtoService.listarAtivos());
        return "pedidos/wizard";
    }

    @PostMapping("/wizard/finalizar")
    public String finalizarWizard(
            @RequestParam Long clienteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataEntrega,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime slotEntrega,
            @RequestParam(required = false) String observacoes,
            @RequestParam(value = "produtoId", required = false) List<Long> produtoIds,
            @RequestParam(value = "quantidade", required = false) List<Integer> quantidades,
            RedirectAttributes ra) {
        try {
            Pedido pedido = pedidoService.criarComItens(
                    clienteId, dataEntrega, slotEntrega, observacoes, produtoIds, quantidades);
            ra.addFlashAttribute("sucesso",
                    "Pedido #" + pedido.getId() + " criado com sucesso!");
            return "redirect:/pedidos/" + pedido.getId();
        } catch (Exception e) {
            ra.addFlashAttribute("erro", e.getMessage());
            return "redirect:/pedidos/wizard";
        }
    }

    // -----------------------------------------------------------------------
    // Criar / Editar
    // -----------------------------------------------------------------------

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("form", new PedidoForm());
        model.addAttribute("clientes", clienteService.listarTodos());
        return "pedidos/form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        Pedido pedido = pedidoService.buscarPorId(id);
        PedidoForm form = new PedidoForm();
        form.setId(pedido.getId());
        form.setClienteId(pedido.getCliente().getId());
        form.setDataEntrega(pedido.getDataEntrega());
        form.setObservacoes(pedido.getObservacoes());
        model.addAttribute("form", form);
        model.addAttribute("clientes", clienteService.listarTodos());
        return "pedidos/form";
    }

    @PostMapping("/salvar")
    public String salvar(@Valid @ModelAttribute("form") PedidoForm form,
                         BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("clientes", clienteService.listarTodos());
            return "pedidos/form";
        }
        try {
            Pedido pedido = (form.getId() == null)
                ? pedidoService.criar(form)
                : pedidoService.atualizar(form.getId(), form);
            ra.addFlashAttribute("sucesso", "Pedido salvo! Agora adicione os produtos.");
            return "redirect:/pedidos/" + pedido.getId();
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("erro", e.getMessage());
            return "redirect:/pedidos";
        }
    }

    // -----------------------------------------------------------------------
    // Detalhe
    // -----------------------------------------------------------------------

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Model model) {
        Pedido pedido = pedidoService.buscarPorId(id);
        BigDecimal totalPago = pedidoService.totalPago(id);
        BigDecimal saldo = pedido.getTotalPedido().subtract(totalPago);

        // Recalcula o total baseado nos subtotais dos itens (workaround para sincronização)
        BigDecimal totalRecalculado = pedido.getItens().stream()
            .map(i -> i.getSubtotal() != null ? i.getSubtotal() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalRecalculado.compareTo(pedido.getTotalPedido()) != 0) {
            pedido.setTotalPedido(totalRecalculado);
            pedidoService.salvarSemRecalculo(pedido);
        }

        model.addAttribute("pedido", pedido);
        model.addAttribute("pagamentos", pedidoService.listarPagamentos(id));
        model.addAttribute("totalPago", totalPago);
        model.addAttribute("saldo", saldo);
        model.addAttribute("produtos", produtoService.listarAtivos());
        model.addAttribute("itemForm", new ItemPedidoForm());
        model.addAttribute("pagForm", new PagamentoForm());
        model.addAttribute("tiposPagamento", TipoPagamento.values());
        model.addAttribute("ordens", producaoService.listarPorPedido(id));
        return "pedidos/detalhe";
    }

    // -----------------------------------------------------------------------
    // Itens
    // -----------------------------------------------------------------------

    @PostMapping("/{id}/item")
    public String adicionarItem(@PathVariable Long id,
                                @Valid @ModelAttribute("itemForm") ItemPedidoForm form,
                                BindingResult result,
                                RedirectAttributes ra) {
        if (result.hasErrors()) {
            ra.addFlashAttribute("erro", "Produto e quantidade são obrigatórios.");
            return "redirect:/pedidos/" + id;
        }
        try {
            pedidoService.adicionarItem(id, form.getProdutoId(), form.getQuantidade());
            ra.addFlashAttribute("sucesso", "Produto adicionado ao pedido!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/pedidos/" + id;
    }

    @PostMapping("/{id}/item/{itemId}/excluir")
    public String removerItem(@PathVariable Long id,
                              @PathVariable Long itemId,
                              RedirectAttributes ra) {
        try {
            pedidoService.removerItem(id, itemId);
            ra.addFlashAttribute("sucesso", "Item removido.");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/pedidos/" + id;
    }

    // -----------------------------------------------------------------------
    // Máquina de estados
    // -----------------------------------------------------------------------

    @PostMapping("/{id}/confirmar")
    public String confirmar(@PathVariable Long id, RedirectAttributes ra) {
        try {
            pedidoService.confirmar(id);
            ra.addFlashAttribute("sucesso", "Pedido confirmado!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/pedidos/" + id;
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable Long id, RedirectAttributes ra) {
        try {
            pedidoService.cancelar(id);
            ra.addFlashAttribute("sucesso", "Pedido cancelado.");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/pedidos/" + id;
    }

    @PostMapping("/{id}/pronto")
    public String marcarPronto(@PathVariable Long id, RedirectAttributes ra) {
        try {
            pedidoService.marcarPronto(id);
            ra.addFlashAttribute("sucesso", "Pedido marcado como PRONTO!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/pedidos/" + id;
    }

    @PostMapping("/{id}/entrega")
    public String registrarEntrega(@PathVariable Long id, RedirectAttributes ra) {
        try {
            pedidoService.registrarEntrega(id);
            ra.addFlashAttribute("sucesso", "Entrega registrada com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/pedidos/" + id;
    }

    // -----------------------------------------------------------------------
    // Pagamentos
    // -----------------------------------------------------------------------

    @PostMapping("/{id}/pagamento")
    public String registrarPagamento(@PathVariable Long id,
                                     @Valid @ModelAttribute("pagForm") PagamentoForm form,
                                     BindingResult result,
                                     RedirectAttributes ra) {
        if (result.hasErrors()) {
            ra.addFlashAttribute("erro", "Verifique os campos de pagamento.");
            return "redirect:/pedidos/" + id;
        }
        try {
            pedidoService.registrarPagamento(id, form);
            ra.addFlashAttribute("sucesso", "Pagamento registrado!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/pedidos/" + id;
    }
}
