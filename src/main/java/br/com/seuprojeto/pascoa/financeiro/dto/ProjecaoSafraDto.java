package br.com.seuprojeto.pascoa.financeiro.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class ProjecaoSafraDto {
    private LocalDate inicio;
    private LocalDate fim;

    private BigDecimal faturamentoProjetado;
    private BigDecimal custoProjetado;
    private BigDecimal impostos;              // aliquota × faturamento
    private BigDecimal lucroProjetado;        // faturamento - custo - impostos
    private BigDecimal metaFaturamento;
    private BigDecimal percentualMeta;        // faturamento / meta × 100

    private long pedidosAtivos;
    private long pedidosEntregues;
    private long pedidosCancelados;
}
