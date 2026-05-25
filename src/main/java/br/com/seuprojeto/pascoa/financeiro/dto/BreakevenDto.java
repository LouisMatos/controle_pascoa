package br.com.seuprojeto.pascoa.financeiro.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BreakevenDto {
    private BigDecimal totalDespesasFixasMensais;
    private BigDecimal precoMedioVenda;
    private BigDecimal custoMedioVariavel;
    private BigDecimal margemContribuicao;    // precoMedio - custoMedio
    private BigDecimal pontoEquilibrio;       // unidades/mês para cobrir fixos
    private long      unidadesVendidasMes;   // atual (pedidos não cancelados mês corrente)
    private boolean   acimaEquilibrio;
    private BigDecimal percentualAtingido;   // unidades / pontoEquilibrio × 100
    private long      unidadesFaltando;      // pontoEquilibrio - unidadesVendidas (0 se acima)
}
