package br.com.seuprojeto.pascoa.inventory.adapter.in.rest.dto;

import br.com.seuprojeto.pascoa.inventory.domain.model.Unidade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record MateriaPrimaRequest(
        @NotBlank String nome,
        @NotNull Unidade unidade,
        @PositiveOrZero BigDecimal estoqueInicial,
        @PositiveOrZero BigDecimal estoqueMinimo,
        Long fornecedorId
) {}
