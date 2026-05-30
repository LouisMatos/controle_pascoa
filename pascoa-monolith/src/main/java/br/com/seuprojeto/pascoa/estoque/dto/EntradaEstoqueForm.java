package br.com.seuprojeto.pascoa.estoque.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EntradaEstoqueForm {

    @NotNull(message = "Selecione a matéria-prima")
    private Long materiaPrimaId;

    @NotNull(message = "Quantidade é obrigatória")
    @DecimalMin(value = "0.001", message = "Quantidade deve ser maior que zero")
    private BigDecimal quantidade;

    @NotNull(message = "Custo unitário é obrigatório")
    @DecimalMin(value = "0.0001", message = "Custo deve ser positivo")
    private BigDecimal custoUnitario;

    @Size(max = 300)
    private String motivo;
}
