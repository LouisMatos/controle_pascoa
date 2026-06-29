package br.com.seuprojeto.pascoa.product.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AtributoRequest(
        @NotBlank @Size(max = 80)  String chave,
        @Size(max = 500)           String valor
) {}
