package br.com.seuprojeto.pascoa.cadastro.controller;

import br.com.seuprojeto.pascoa.cadastro.service.ClienteService;
import br.com.seuprojeto.pascoa.cadastro.service.MateriaPrimaService;
import br.com.seuprojeto.pascoa.cadastro.service.ProdutoService;
import br.com.seuprojeto.pascoa.financeiro.entity.ConfiguracaoFinanceira;
import br.com.seuprojeto.pascoa.financeiro.repository.ConfiguracaoFinanceiraRepository;
import br.com.seuprojeto.pascoa.pedido.entity.StatusPedido;
import br.com.seuprojeto.pascoa.pedido.repository.PagamentoRepository;
import br.com.seuprojeto.pascoa.pedido.repository.PedidoRepository;
import br.com.seuprojeto.pascoa.producao.entity.StatusOrdem;
import br.com.seuprojeto.pascoa.producao.repository.OrdemProducaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final ClienteService clienteService;
    private final ProdutoService produtoService;
    private final MateriaPrimaService materiaPrimaService;
    private final PedidoRepository pedidoRepository;
    private final OrdemProducaoRepository ordemRepository;
    private final PagamentoRepository pagamentoRepository;
    private final ConfiguracaoFinanceiraRepository configFinanceiraRepository;

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {

        // ── Estoque ──────────────────────────────────────────────────────────
        var mpsCriticas = materiaPrimaService.listarComEstoqueCritico();

        // ── KPIs de pedidos ───────────────────────────────────────────────────
        var statusAtivos = List.of(
                StatusPedido.NOVO, StatusPedido.CONFIRMADO,
                StatusPedido.EM_PRODUCAO, StatusPedido.PRONTO);
        long pedidosAbertos       = pedidoRepository.countByStatusIn(statusAtivos);
        BigDecimal faturamentoAberto = pedidoRepository.sumTotalPorStatuses(statusAtivos);

        // ── KPIs de produção ──────────────────────────────────────────────────
        long ordensPendentes   = ordemRepository.countByStatus(StatusOrdem.PENDENTE);
        long ordensEmAndamento = ordemRepository.countByStatus(StatusOrdem.EM_ANDAMENTO);

        // ── KPIs financeiros ──────────────────────────────────────────────────
        BigDecimal totalRecebido = pagamentoRepository.sumTotal();
        ConfiguracaoFinanceira config = configFinanceiraRepository.obter();
        BigDecimal meta = config.getMetaFaturamentoMensal();

        // pctMeta: null = meta não definida; int 0–100 = percentage
        Integer pctMeta = null;
        if (meta.compareTo(BigDecimal.ZERO) > 0) {
            pctMeta = totalRecebido
                    .divide(meta, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP)
                    .intValue();
            pctMeta = Math.min(pctMeta, 100);
        }

        boolean metaDefinida = meta.compareTo(BigDecimal.ZERO) > 0;

        // ── Model ─────────────────────────────────────────────────────────────
        model.addAttribute("totalClientes",      clienteService.listarTodos().size());
        model.addAttribute("totalProdutos",      produtoService.listarAtivos().size());
        model.addAttribute("alertasEstoque",     mpsCriticas.size());
        model.addAttribute("mpsCriticas",        mpsCriticas);

        model.addAttribute("pedidosAbertos",     pedidosAbertos);
        model.addAttribute("faturamentoAberto",  faturamentoAberto);

        model.addAttribute("ordensPendentes",    ordensPendentes);
        model.addAttribute("ordensEmAndamento",  ordensEmAndamento);

        model.addAttribute("totalRecebido",      totalRecebido);
        model.addAttribute("metaMensal",         meta);
        model.addAttribute("metaDefinida",       metaDefinida);
        model.addAttribute("pctMeta",            pctMeta);

        return "dashboard";
    }
}
