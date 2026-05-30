package br.com.seuprojeto.pascoa.product.adapter.in.rest.dto;

import br.com.seuprojeto.pascoa.product.domain.model.Categoria;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ProdutoRequest(
        @NotBlank String nome,
        String descricao,
        @NotNull @Positive BigDecimal preco,
        @NotNull Categoria categoria,
        String fotoUrl
) {}
