package br.com.seuprojeto.pascoa.auth.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String login,
        @NotBlank String senha,
        Integer totpCodigo
) {}
