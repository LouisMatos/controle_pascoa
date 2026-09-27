package br.com.seuprojeto.pascoa.financial.adapter.in.rest.dto;

import br.com.seuprojeto.pascoa.financial.domain.model.CategoriaLancamento;
import br.com.seuprojeto.pascoa.financial.domain.model.TipoLancamento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LancamentoRequest(
        @NotNull TipoLancamento tipo,
        @NotNull CategoriaLancamento categoria,
        @NotBlank String descricao,
        @NotNull @Positive BigDecimal valor,
        LocalDate data,
        String referenciaId
) {}
