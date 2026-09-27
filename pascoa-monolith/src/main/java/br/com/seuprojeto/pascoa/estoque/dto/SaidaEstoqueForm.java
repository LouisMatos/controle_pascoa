package br.com.seuprojeto.pascoa.estoque.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SaidaEstoqueForm {

    @NotNull(message = "Selecione a matéria-prima")
    private Long materiaPrimaId;

    @NotNull(message = "Quantidade é obrigatória")
    @DecimalMin(value = "0.001", message = "Quantidade deve ser maior que zero")
    private BigDecimal quantidade;

    @NotBlank(message = "Informe o motivo da saída")
    @Size(max = 300)
    private String motivo;
}
