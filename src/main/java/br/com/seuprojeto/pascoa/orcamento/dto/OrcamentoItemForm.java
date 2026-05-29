package br.com.seuprojeto.pascoa.orcamento.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrcamentoItemForm {

    @NotNull(message = "Selecione um produto")
    private Long produtoId;

    @NotNull(message = "Quantidade é obrigatória")
    @Min(value = 1, message = "Quantidade mínima é 1")
    private Integer quantidade;

    // Opcional: se preenchido, deve ser positivo; null = usar preço de venda do produto
    @DecimalMin(value = "0.01", message = "Preço unitário deve ser maior que zero")
    private BigDecimal precoUnitario;
}
