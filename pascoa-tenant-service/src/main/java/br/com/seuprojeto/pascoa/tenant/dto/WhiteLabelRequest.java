package br.com.seuprojeto.pascoa.tenant.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record WhiteLabelRequest(
        @Size(max = 100) String nomeApp,
        @Size(max = 500) String logoUrl,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Cor deve estar em hex #RRGGBB") String corPrimaria,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Cor deve estar em hex #RRGGBB") String corSecundaria,
        @Size(max = 500) String favicon,
        @Size(max = 500) String rodape
) {}
