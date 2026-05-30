package br.com.seuprojeto.pascoa.gastos.dto;

import br.com.seuprojeto.pascoa.gastos.entity.CategoriaGasto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class GastoVariavelForm {

    @NotBlank(message = "Descrição é obrigatória")
    @Size(max = 200)
    private String descricao;

    @NotNull(message = "Valor é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
    private BigDecimal valor;

    @NotNull(message = "Data do lançamento é obrigatória")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataLancamento;

    @NotNull(message = "Categoria é obrigatória")
    private CategoriaGasto categoria;

    @Size(max = 500)
    private String observacoes;
}
