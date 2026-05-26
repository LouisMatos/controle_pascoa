package br.com.seuprojeto.pascoa.analytics.service;

import br.com.seuprojeto.pascoa.analytics.dto.*;
import br.com.seuprojeto.pascoa.pedido.repository.ItemPedidoRepository;
import br.com.seuprojeto.pascoa.pedido.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final PedidoRepository pedidoRepository;
    private final ItemPedidoRepository itemPedidoRepository;

    /** Lista de anos com pedidos (DESC). Inclui o ano atual mesmo sem pedidos. */
    public List<Integer> anosDisponiveis() {
        List<Integer> anos = new ArrayList<>(pedidoRepository.anosComPedidos());
        int anoAtual = LocalDate.now().getYear();
        if (!anos.contains(anoAtual)) anos.add(0, anoAtual);
        return anos;
    }

    /** Dados completos de uma safra (ano). */
    public SafraDto safra(int ano) {
        BigDecimal total = pedidoRepository.totalPorAno(ano);
        long count      = pedidoRepository.countPorAno(ano);

        List<Object[]> rows = pedidoRepository.faturamentoPorMes(ano);
        Map<Integer, Object[]> porMes = rows.stream()
                .collect(Collectors.toMap(r -> ((Number) r[0]).intValue(), r -> r));

        List<MesDto> meses = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            Object[] row = porMes.get(m);
            if (row != null) {
                meses.add(new MesDto(m,
                        toBigDecimal(row[1]),
                        ((Number) row[2]).longValue()));
            } else {
                meses.add(new MesDto(m, BigDecimal.ZERO, 0L));
            }
        }

        return new SafraDto(ano, total == null ? BigDecimal.ZERO : total, count, meses);
    }

    /** Comparativo entre os dois últimos anos disponíveis (máx). */
    public ComparativoSafraDto comparativo() {
        List<Integer> anos = anosDisponiveis();
        List<SafraDto> safras = anos.stream()
                .limit(2)
                .map(this::safra)
                .collect(Collectors.toList());
        return new ComparativoSafraDto(safras);
    }

    /** Top 15 produtos de um ano. */
    public List<RankingProdutoDto> rankingProdutos(int ano) {
        return itemPedidoRepository.rankingProdutosPorAno(ano).stream()
                .map(r -> new RankingProdutoDto(
                        (String) r[0],
                        r[1] != null ? (String) r[1] : "—",
                        ((Number) r[2]).longValue(),
                        toBigDecimal(r[3])))
                .collect(Collectors.toList());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return BigDecimal.ZERO;
    }
}
