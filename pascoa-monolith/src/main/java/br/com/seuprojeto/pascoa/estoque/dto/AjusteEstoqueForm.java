package br.com.seuprojeto.pascoa.estoque.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AjusteEstoqueForm {

    @NotNull(message = "Selecione a matéria-prima")
    private Long materiaPrimaId;

    @NotNull(message = "Nova quantidade é obrigatória")
    @DecimalMin(value = "0.0", message = "Quantidade não pode ser negativa")
    private BigDecimal novaQuantidade;

    @NotBlank(message = "Justificativa é obrigatória para ajustes de inventário")
    @Size(max = 500)
    private String motivo;
}
