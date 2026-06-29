package br.com.seuprojeto.pascoa.product.adapter.in.rest.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Map;

public record VarianteRequest(
        @Size(max = 50)                              String sku,
        @NotBlank @Size(max = 150)                   String nome,
        @NotNull @DecimalMin(value = "0.01")         BigDecimal preco,
        Map<String, String>                          atributos,
        Boolean                                      disponivel
) {}
