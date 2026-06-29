package br.com.seuprojeto.pascoa.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ApiKeyRequest(
        @NotBlank @Size(max = 100) String nome,
        @Size(max = 500)           String escopos,
        LocalDate                  expiraEm
) {}
