package br.com.seuprojeto.pascoa.production.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FaseRequest(
        @NotBlank @Size(max = 80)  String fase,
        @NotNull                   Integer ordem,
        @Size(max = 80)            String mudadoPor,
        @Size(max = 500)           String observacao
) {}
