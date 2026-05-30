package br.com.seuprojeto.pascoa.auth.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(@NotBlank String refreshToken) {}
