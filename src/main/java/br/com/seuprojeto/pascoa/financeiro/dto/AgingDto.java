package br.com.seuprojeto.pascoa.financeiro.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class AgingDto {
    private BigDecimal totalCorrente;    // não vencido
    private BigDecimal total1a30;
    private BigDecimal total31a60;
    private BigDecimal total61a90;
    private BigDecimal totalAcima90;
    private BigDecimal totalGeral;

    private List<LinhaAgingDto> corrente;
    private List<LinhaAgingDto> atraso1a30;
    private List<LinhaAgingDto> atraso31a60;
    private List<LinhaAgingDto> atraso61a90;
    private List<LinhaAgingDto> atrasoAcima90;

    @Data
    @Builder
    public static class LinhaAgingDto {
        private Long   contaId;
        private String clienteNome;
        private Long   pedidoId;
        private java.time.LocalDate vencimento;
        private int    diasAtraso;
        private BigDecimal saldo;
    }
}
