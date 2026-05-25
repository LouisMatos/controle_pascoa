package br.com.seuprojeto.pascoa.financeiro.dto;

import br.com.seuprojeto.pascoa.financeiro.entity.DespesaVariavel;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CustoRealDto {
    private Long pedidoId;
    private String clienteNome;
    private BigDecimal totalPedido;

    private BigDecimal custoMP;
    private BigDecimal rateioFixo;
    private BigDecimal despesasVariaveis;
    private BigDecimal custoReal;
    private BigDecimal margemReal;        // %
    private BigDecimal margemDesejada;    // % from configuracao

    private List<LinhaCustoMpDto> linhasMP;
    private List<DespesaVariavel> despesasDetalhadas;

    @Data
    @Builder
    public static class LinhaCustoMpDto {
        private String produto;
        private int quantidade;
        private BigDecimal custoUnitario;
        private BigDecimal subtotal;
    }
}
