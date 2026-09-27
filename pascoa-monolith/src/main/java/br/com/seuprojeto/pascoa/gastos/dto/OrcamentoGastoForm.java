package br.com.seuprojeto.pascoa.gastos.dto;

import br.com.seuprojeto.pascoa.gastos.entity.CategoriaGasto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrcamentoGastoForm {

    @NotNull(message = "Categoria é obrigatória")
    private CategoriaGasto categoria;

    @NotNull(message = "Valor orçado é obrigatório")
    @DecimalMin(value = "0.00", message = "Valor não pode ser negativo")
    private BigDecimal valorOrcado;

    @NotNull(message = "Mês é obrigatório")
    @Min(1) @Max(12)
    private Integer referenciaMes;

    @NotNull(message = "Ano é obrigatório")
    @Min(2020)
    private Integer referenciaAno;
}
