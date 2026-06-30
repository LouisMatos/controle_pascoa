package br.com.seuprojeto.pascoa.financeiro.service;

import br.com.seuprojeto.pascoa.estoque.entity.TipoMovimentacao;
import br.com.seuprojeto.pascoa.estoque.repository.MovimentacaoEstoqueRepository;
import br.com.seuprojeto.pascoa.financeiro.dto.FluxoCaixaDto;
import br.com.seuprojeto.pascoa.financeiro.repository.ContaPagarRepository;
import br.com.seuprojeto.pascoa.financeiro.repository.ContaReceberRepository;
import br.com.seuprojeto.pascoa.financeiro.repository.DespesaFixaRepository;
import br.com.seuprojeto.pascoa.gastos.repository.GastoVariavelRepository;
import br.com.seuprojeto.pascoa.pedido.repository.PagamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class FluxoCaixaService {

    private final PagamentoRepository pagamentoRepository;
    private final ContaReceberRepository contaReceberRepository;
    private final ContaPagarRepository contaPagarRepository;
    private final DespesaFixaRepository despesaFixaRepository;
    private final MovimentacaoEstoqueRepository movimentacaoRepository;
    private final GastoVariavelRepository gastoRepository;

    @Transactional(readOnly = true)
    public FluxoCaixaDto calcular(LocalDate inicio, LocalDate fim) {
        // ENTRADA — recebido real: pagamentos no período (agregação no DB, ver V15)
        BigDecimal recebidoReal = pagamentoRepository.sumValorByPeriodo(inicio, fim);

        // ENTRADA — previsto: contas a receber com vencimento no período
        BigDecimal previstoEntrada = contaReceberRepository.sumPrevistoEntrada(inicio, fim);

        // SAÍDA — MP comprada: entradas de estoque com custo no período (agregação no DB, ver V15)
        LocalDateTime inicioDt      = inicio.atStartOfDay();
        LocalDateTime fimExclusivo  = fim.plusDays(1).atStartOfDay();
        BigDecimal saidaMP = movimentacaoRepository.sumCustoByTipoEPeriodo(
                TipoMovimentacao.ENTRADA, inicioDt, fimExclusivo);

        // SAÍDA — despesas fixas proporcionais ao período (normalizado para meses)
        long diasPeriodo = ChronoUnit.DAYS.between(inicio, fim.plusDays(1));
        double fracaoMes = diasPeriodo / 30.44;
        BigDecimal totalFixoMensal = despesaFixaRepository.sumMensalAtivas();
        BigDecimal saidaDespesasFixas = totalFixoMensal
            .multiply(BigDecimal.valueOf(fracaoMes))
            .setScale(2, java.math.RoundingMode.HALF_UP);

        // SAÍDA — gastos variáveis lançados no período
        BigDecimal saidaGastos = gastoRepository.sumTotalByPeriodo(inicio, fim);

        // SAÍDA — previsto: contas a pagar com vencimento no período
        BigDecimal saidaPrevista = contaPagarRepository.sumPrevistoSaida(inicio, fim);

        BigDecimal totalEntradas  = recebidoReal.add(previstoEntrada);
        BigDecimal totalSaidas    = saidaMP.add(saidaDespesasFixas).add(saidaGastos).add(saidaPrevista);
        BigDecimal saldoRealizado = recebidoReal.subtract(saidaMP).subtract(saidaDespesasFixas).subtract(saidaGastos);
        BigDecimal saldoProjetado = totalEntradas.subtract(totalSaidas);

        return FluxoCaixaDto.builder()
            .inicio(inicio)
            .fim(fim)
            .recebidoReal(recebidoReal)
            .previstoEntrada(previstoEntrada)
            .saidaMP(saidaMP)
            .saidaDespesasFixas(saidaDespesasFixas)
            .saidaGastosVariaveis(saidaGastos)
            .saidaPrevista(saidaPrevista)
            .totalEntradas(totalEntradas)
            .totalSaidas(totalSaidas)
            .saldoRealizado(saldoRealizado)
            .saldoProjetado(saldoProjetado)
            .build();
    }

    public FluxoCaixaDto calcularMesAtual() {
        YearMonth mes = YearMonth.now();
        return calcular(mes.atDay(1), mes.atEndOfMonth());
    }
}
