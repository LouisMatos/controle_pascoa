package br.com.seuprojeto.pascoa.financeiro.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class FluxoCaixaDto {
    private LocalDate inicio;
    private LocalDate fim;

    // Entradas
    private BigDecimal recebidoReal;       // Pagamentos recebidos no período
    private BigDecimal previstoEntrada;    // ContaReceber a vencer no período

    // Saídas
    private BigDecimal saidaMP;               // Compras de MP registradas no período
    private BigDecimal saidaDespesasFixas;    // Despesas fixas proporcionais ao período
    private BigDecimal saidaGastosVariaveis;  // Gastos variáveis lançados no período
    private BigDecimal saidaPrevista;         // ContaPagar a vencer no período

    // Totais
    private BigDecimal totalEntradas;
    private BigDecimal totalSaidas;
    private BigDecimal saldoRealizado;     // recebidoReal - (saidaMP + saidaDespesasFixas)
    private BigDecimal saldoProjetado;     // totalEntradas - totalSaidas
}
