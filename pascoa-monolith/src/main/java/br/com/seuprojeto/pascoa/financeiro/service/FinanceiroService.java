package br.com.seuprojeto.pascoa.financeiro.service;

import br.com.seuprojeto.pascoa.cadastro.repository.MateriaPrimaRepository;
import br.com.seuprojeto.pascoa.fichaTecnica.repository.FichaTecnicaRepository;
import br.com.seuprojeto.pascoa.financeiro.dto.MargemProdutoDto;
import br.com.seuprojeto.pascoa.financeiro.dto.RelatorioFinanceiro;
import br.com.seuprojeto.pascoa.financeiro.dto.TopProdutoDto;
import br.com.seuprojeto.pascoa.financeiro.repository.DespesaFixaRepository;
import br.com.seuprojeto.pascoa.gastos.repository.GastoVariavelRepository;
import br.com.seuprojeto.pascoa.pedido.entity.StatusPedido;
import br.com.seuprojeto.pascoa.pedido.repository.ItemPedidoRepository;
import br.com.seuprojeto.pascoa.pedido.repository.PagamentoRepository;
import br.com.seuprojeto.pascoa.pedido.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FinanceiroService {

    private final PedidoRepository pedidoRepository;
    private final PagamentoRepository pagamentoRepository;
    private final ItemPedidoRepository itemPedidoRepository;
    private final MateriaPrimaRepository materiaPrimaRepository;
    private final FichaTecnicaRepository fichaTecnicaRepository;
    private final GastoVariavelRepository gastoRepository;
    private final DespesaFixaRepository despesaFixaRepository;

    @Transactional(readOnly = true)
    public RelatorioFinanceiro gerarRelatorio() {
        BigDecimal faturamentoEntregues = safe(pedidoRepository.sumTotalPorStatus(StatusPedido.ENTREGUE));
        BigDecimal totalRecebido        = safe(pagamentoRepository.sumTotal());
        BigDecimal pipeline             = safe(pedidoRepository.sumTotalPorStatuses(List.of(
            StatusPedido.CONFIRMADO, StatusPedido.EM_PRODUCAO, StatusPedido.PRONTO)));

        YearMonth mesAtual = YearMonth.now();
        BigDecimal gastosVariaveisMes  = safe(gastoRepository.sumTotal(mesAtual.getYear(), mesAtual.getMonthValue()));
        BigDecimal despesasFixasMensais = safe(despesaFixaRepository.sumMensalAtivas());

        List<Object[]> topRows = itemPedidoRepository.topProdutos(StatusPedido.CANCELADO);
        List<TopProdutoDto> topProdutos = topRows.stream()
            .limit(5)
            .map(r -> new TopProdutoDto(
                (String) r[0],
                ((Number) r[1]).longValue(),
                r[2] != null ? (BigDecimal) r[2] : BigDecimal.ZERO))
            .toList();

        List<MargemProdutoDto> margens = calcularMargens();

        return RelatorioFinanceiro.builder()
            .faturamentoEntregues(faturamentoEntregues)
            .totalRecebido(totalRecebido)
            .pipeline(pipeline)
            .totalGastosVariaveisMes(gastosVariaveisMes)
            .totalDespesasFixasMensais(despesasFixasMensais)
            .totalPedidos(pedidoRepository.count())
            .pedidosNovos(pedidoRepository.countByStatus(StatusPedido.NOVO))
            .pedidosConfirmados(pedidoRepository.countByStatus(StatusPedido.CONFIRMADO))
            .pedidosEmProducao(pedidoRepository.countByStatus(StatusPedido.EM_PRODUCAO))
            .pedidosProntos(pedidoRepository.countByStatus(StatusPedido.PRONTO))
            .pedidosEntregues(pedidoRepository.countByStatus(StatusPedido.ENTREGUE))
            .pedidosCancelados(pedidoRepository.countByStatus(StatusPedido.CANCELADO))
            .materiaPrimasCriticas(materiaPrimaRepository.findComEstoqueCritico())
            .topProdutos(topProdutos)
            .margens(margens)
            .build();
    }

    private List<MargemProdutoDto> calcularMargens() {
        return fichaTecnicaRepository.findAllComItens().stream()
            .map(f -> {
                BigDecimal custoTotal = f.getItens().stream()
                    .map(i -> i.getQuantidade().multiply(i.getMateriaPrima().getCustoUnitario()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal rendimento = f.getRendimento();
                BigDecimal custoUnit = rendimento.compareTo(BigDecimal.ZERO) > 0
                    ? custoTotal.divide(rendimento, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
                BigDecimal preco = f.getProduto().getPrecoVenda();
                BigDecimal margemPct = preco.compareTo(BigDecimal.ZERO) > 0
                    ? preco.subtract(custoUnit)
                        .divide(preco, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
                return new MargemProdutoDto(f.getProduto().getNome(), preco, custoUnit, margemPct);
            })
            .sorted(Comparator.comparing(MargemProdutoDto::margemPct).reversed())
            .toList();
    }

    private BigDecimal safe(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
