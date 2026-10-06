package br.com.seuprojeto.pascoa.financeiro.service;

import br.com.seuprojeto.pascoa.financeiro.dto.BreakevenDto;
import br.com.seuprojeto.pascoa.financeiro.dto.ProjecaoSafraDto;
import br.com.seuprojeto.pascoa.financeiro.dto.AgingDto;
import br.com.seuprojeto.pascoa.financeiro.repository.ConfiguracaoFinanceiraRepository;
import br.com.seuprojeto.pascoa.financeiro.repository.DespesaFixaRepository;
import br.com.seuprojeto.pascoa.gastos.repository.GastoVariavelRepository;
import br.com.seuprojeto.pascoa.pedido.entity.StatusPedido;
import br.com.seuprojeto.pascoa.pedido.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BreakevenService {

    private final DespesaFixaRepository despesaFixaRepository;
    private final PedidoRepository pedidoRepository;
    private final ConfiguracaoFinanceiraRepository configuracaoRepository;
    private final GastoVariavelRepository gastoRepository;

    @Transactional(readOnly = true)
    public BreakevenDto calcular() {
        // SUM() retorna null quando não há linhas — coalescer para zero evita NPE nas operações seguintes
        BigDecimal despesasFixasPuras = Objects.requireNonNullElse(
            despesaFixaRepository.sumMensalAtivas(), BigDecimal.ZERO);

        YearMonth mesAtual0 = YearMonth.now();
        BigDecimal gastosVariaveisMes = Objects.requireNonNullElse(
            gastoRepository.sumTotal(mesAtual0.getYear(), mesAtual0.getMonthValue()), BigDecimal.ZERO);

        BigDecimal totalFixoMensal = despesasFixasPuras.add(gastosVariaveisMes);

        YearMonth mesAtual = YearMonth.now();
        LocalDate inicioMes = mesAtual.atDay(1);
        LocalDate fimMes    = mesAtual.atEndOfMonth();

        var pedidosMes = pedidoRepository.findAllComCliente().stream()
            .filter(p -> p.getStatus() != StatusPedido.CANCELADO)
            .filter(p -> {
                var d = p.getDataPedido().toLocalDate();
                return !d.isBefore(inicioMes) && !d.isAfter(fimMes);
            })
            .toList();

        BigDecimal totalUnidades = pedidosMes.stream()
            .flatMap(p -> p.getItens().stream())
            .map(i -> i.getQuantidade())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal faturamentoMes = pedidosMes.stream()
            .map(p -> p.getTotalPedido())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal precoMedioVenda = totalUnidades.signum() > 0
            ? faturamentoMes.divide(totalUnidades, 4, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        // Custo médio variável: custoRealCalculado / unidades (fallback: 60% do preço)
        BigDecimal custoMedioVariavel = pedidosMes.stream()
            .filter(p -> p.getCustoRealCalculado() != null)
            .map(p -> p.getCustoRealCalculado())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pedidosComCusto = pedidosMes.stream()
            .filter(p -> p.getCustoRealCalculado() != null).count();

        if (pedidosComCusto > 0 && totalUnidades.signum() > 0) {
            custoMedioVariavel = custoMedioVariavel.divide(
                totalUnidades, 4, RoundingMode.HALF_UP);
        } else if (precoMedioVenda.compareTo(BigDecimal.ZERO) > 0) {
            custoMedioVariavel = precoMedioVenda.multiply(new BigDecimal("0.60"));
        }

        BigDecimal margemContribuicao = precoMedioVenda.subtract(custoMedioVariavel);

        BigDecimal pontoEquilibrio = BigDecimal.ZERO;
        if (margemContribuicao.compareTo(BigDecimal.ZERO) > 0) {
            pontoEquilibrio = totalFixoMensal.divide(margemContribuicao, 1, RoundingMode.CEILING);
        }

        BigDecimal pctAtingido = pontoEquilibrio.compareTo(BigDecimal.ZERO) > 0
            ? totalUnidades
                .divide(pontoEquilibrio, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        boolean acima = totalUnidades.compareTo(pontoEquilibrio) >= 0;
        long faltando = acima ? 0L
            : pontoEquilibrio.subtract(totalUnidades)
                .setScale(0, RoundingMode.CEILING).longValue();

        return BreakevenDto.builder()
            .totalDespesasFixasMensais(totalFixoMensal)
            .despesasFixasPuras(despesasFixasPuras)
            .gastosVariaveisMes(gastosVariaveisMes)
            .precoMedioVenda(precoMedioVenda.setScale(2, RoundingMode.HALF_UP))
            .custoMedioVariavel(custoMedioVariavel.setScale(2, RoundingMode.HALF_UP))
            .margemContribuicao(margemContribuicao.setScale(2, RoundingMode.HALF_UP))
            .pontoEquilibrio(pontoEquilibrio)
            .unidadesVendidasMes(totalUnidades.setScale(0, RoundingMode.HALF_UP).longValue())
            .acimaEquilibrio(acima)
            .percentualAtingido(pctAtingido)
            .unidadesFaltando(faltando)
            .build();
    }

    @Transactional(readOnly = true)
    public ProjecaoSafraDto projecaoSafra(LocalDate inicio, LocalDate fim) {
        var config = configuracaoRepository.obter();

        var pedidos = pedidoRepository.findAllComCliente().stream()
            .filter(p -> {
                var d = p.getDataPedido().toLocalDate();
                return !d.isBefore(inicio) && !d.isAfter(fim);
            })
            .toList();

        BigDecimal faturamento = pedidos.stream()
            .filter(p -> p.getStatus() != StatusPedido.CANCELADO)
            .map(p -> p.getTotalPedido())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal custoProjetado = pedidos.stream()
            .filter(p -> p.getCustoRealCalculado() != null && p.getStatus() != StatusPedido.CANCELADO)
            .map(p -> p.getCustoRealCalculado())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal impostos = faturamento.multiply(
            config.getAliquotaSimples().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP))
            .setScale(2, RoundingMode.HALF_UP);

        BigDecimal lucro = faturamento.subtract(custoProjetado).subtract(impostos);

        BigDecimal meta = config.getMetaFaturamentoMensal();
        BigDecimal pctMeta = meta.compareTo(BigDecimal.ZERO) > 0
            ? faturamento.divide(meta, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        return ProjecaoSafraDto.builder()
            .inicio(inicio)
            .fim(fim)
            .faturamentoProjetado(faturamento)
            .custoProjetado(custoProjetado)
            .impostos(impostos)
            .lucroProjetado(lucro)
            .metaFaturamento(meta)
            .percentualMeta(pctMeta)
            .pedidosAtivos(pedidos.stream().filter(p -> p.getStatus() != StatusPedido.CANCELADO
                && p.getStatus() != StatusPedido.ENTREGUE).count())
            .pedidosEntregues(pedidos.stream().filter(p -> p.getStatus() == StatusPedido.ENTREGUE).count())
            .pedidosCancelados(pedidos.stream().filter(p -> p.getStatus() == StatusPedido.CANCELADO).count())
            .build();
    }

    @Transactional(readOnly = true)
    public AgingDto aging() {
        LocalDate hoje = LocalDate.now();

        List<AgingDto.LinhaAgingDto> corrente = new ArrayList<>();
        List<AgingDto.LinhaAgingDto> a1a30    = new ArrayList<>();
        List<AgingDto.LinhaAgingDto> a31a60   = new ArrayList<>();
        List<AgingDto.LinhaAgingDto> a61a90   = new ArrayList<>();
        List<AgingDto.LinhaAgingDto> aAcima90 = new ArrayList<>();

        for (Object[] linha : pedidoRepository.saldosEmAberto()) {
            Long pedidoId = (Long) linha[0];
            String clienteNome = linha[1] != null ? (String) linha[1] : "(cliente removido)";
            LocalDate vencimento = linha[2] != null
                ? (LocalDate) linha[2]
                : ((LocalDateTime) linha[3]).toLocalDate();
            BigDecimal saldo = (BigDecimal) linha[4];

            long dias = ChronoUnit.DAYS.between(vencimento, hoje);
            var dto = AgingDto.LinhaAgingDto.builder()
                .clienteNome(clienteNome)
                .pedidoId(pedidoId)
                .vencimento(vencimento)
                .diasAtraso((int) Math.max(0, dias))
                .saldo(saldo)
                .build();

            if (dias <= 0) {
                corrente.add(dto);
            } else if (dias <= 30) {
                a1a30.add(dto);
            } else if (dias <= 60) {
                a31a60.add(dto);
            } else if (dias <= 90) {
                a61a90.add(dto);
            } else {
                aAcima90.add(dto);
            }
        }

        BigDecimal tCorrente = soma(corrente);
        BigDecimal t1a30     = soma(a1a30);
        BigDecimal t31a60    = soma(a31a60);
        BigDecimal t61a90    = soma(a61a90);
        BigDecimal tAcima90  = soma(aAcima90);

        return AgingDto.builder()
            .corrente(corrente).atraso1a30(a1a30).atraso31a60(a31a60)
            .atraso61a90(a61a90).atrasoAcima90(aAcima90)
            .totalCorrente(tCorrente).total1a30(t1a30).total31a60(t31a60)
            .total61a90(t61a90).totalAcima90(tAcima90)
            .totalGeral(tCorrente.add(t1a30).add(t31a60).add(t61a90).add(tAcima90))
            .build();
    }

    private BigDecimal soma(List<AgingDto.LinhaAgingDto> lista) {
        return lista.stream().map(AgingDto.LinhaAgingDto::getSaldo)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
