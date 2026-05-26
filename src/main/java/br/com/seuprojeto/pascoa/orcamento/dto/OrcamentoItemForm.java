package br.com.seuprojeto.pascoa.orcamento.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrcamentoItemForm {
    private Long produtoId;
    private Integer quantidade;
    private BigDecimal precoUnitario;
}
