package br.com.seuprojeto.pascoa.fichaTecnica.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ItemFichaForm {

    @NotNull(message = "Selecione uma matéria-prima")
    private Long materiaPrimaId;

    @NotNull(message = "Quantidade é obrigatória")
    @DecimalMin(value = "0.001", message = "Quantidade deve ser maior que zero")
    private BigDecimal quantidade;
}
