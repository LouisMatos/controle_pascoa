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
import br.com.seuprojeto.pascoa.qualidade.repository.InspecaoRepository;
import br.com.seuprojeto.pascoa.gastos.repository.GastoVariavelRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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
    private final InspecaoRepository inspecaoRepository;
    private final GastoVariavelRepository gastoRepository;

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

        LocalDate hoje = LocalDate.now();
        var statusOrdemAberta = List.of(StatusOrdem.PENDENTE, StatusOrdem.EM_ANDAMENTO);
        var top = PageRequest.of(0, 10);
        var entregarHoje = pedidoRepository.findPorDataEntrega(hoje, statusAtivos, top);
        var atrasados    = pedidoRepository.findAtrasados(hoje, statusAtivos, top);
        var produzir     = ordemRepository.findAbertasPorPrazo(statusOrdemAberta, top);
        long totalEntregarHoje = pedidoRepository.countByDataEntregaAndStatusIn(hoje, statusAtivos);
        long totalAtrasados    = pedidoRepository.countByDataEntregaBeforeAndStatusIn(hoje, statusAtivos);
        long totalProduzir     = ordemRepository.countByStatusIn(statusOrdemAberta);

        // ── KPIs financeiros ──────────────────────────────────────────────────
        BigDecimal totalRecebido = pagamentoRepository.sumTotal();
        ConfiguracaoFinanceira config = configFinanceiraRepository.obter();
        BigDecimal meta = config.getMetaFaturamentoMensal();

        // Receita do mês (pedidos entregues)
        var mesAtual = YearMonth.now();
        BigDecimal receitaMes = pedidoRepository.sumTotalPorStatusAndMes(
                List.of(StatusPedido.ENTREGUE),
                mesAtual.getMonthValue(),
                mesAtual.getYear());
        if (receitaMes == null) receitaMes = BigDecimal.ZERO;

        // Gastos do mês
        BigDecimal gastosMes = gastoRepository.sumTotal(mesAtual.getYear(), mesAtual.getMonthValue());
        if (gastosMes == null) gastosMes = BigDecimal.ZERO;

        // Margem bruta do mês
        Integer pctMargemMes = null;
        if (receitaMes.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal margem = receitaMes.subtract(gastosMes);
            pctMargemMes = margem
                    .divide(receitaMes, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP)
                    .intValue();
        }

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

        // ── KPIs de qualidade ─────────────────────────────────────────────────
        long inspeccoesPendentes = inspecaoRepository.countPendentes();

        // ── Model ─────────────────────────────────────────────────────────────
        model.addAttribute("totalClientes",      clienteService.listarTodos().size());
        model.addAttribute("totalProdutos",      produtoService.listarAtivos().size());
        model.addAttribute("alertasEstoque",     mpsCriticas.size());
        model.addAttribute("mpsCriticas",        mpsCriticas);

        model.addAttribute("pedidosAbertos",     pedidosAbertos);
        model.addAttribute("faturamentoAberto",  faturamentoAberto);

        model.addAttribute("entregarHoje",       entregarHoje);
        model.addAttribute("atrasados",          atrasados);
        model.addAttribute("produzir",           produzir);
        model.addAttribute("totalEntregarHoje",  totalEntregarHoje);
        model.addAttribute("totalAtrasados",     totalAtrasados);
        model.addAttribute("totalProduzir",      totalProduzir);

        model.addAttribute("ordensPendentes",    ordensPendentes);
        model.addAttribute("ordensEmAndamento",  ordensEmAndamento);
        model.addAttribute("inspeccoesPendentes", inspeccoesPendentes);

        model.addAttribute("totalRecebido",      totalRecebido);
        model.addAttribute("receitaMes",         receitaMes);
        model.addAttribute("gastosMes",          gastosMes);
        model.addAttribute("pctMargemMes",       pctMargemMes);
        model.addAttribute("metaMensal",         meta);
        model.addAttribute("metaDefinida",       metaDefinida);
        model.addAttribute("pctMeta",            pctMeta);

        return "dashboard";
    }
}
