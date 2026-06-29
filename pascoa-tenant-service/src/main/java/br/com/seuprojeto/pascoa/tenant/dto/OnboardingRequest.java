package br.com.seuprojeto.pascoa.tenant.dto;

import br.com.seuprojeto.pascoa.tenant.domain.TipoNegocio;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record OnboardingRequest(
        @NotBlank @Pattern(regexp = "[a-z0-9-]{3,50}", message = "id deve ser slug minúsculo, 3-50 chars")
        String tenantId,
        @NotBlank @Size(max = 200) String razaoSocial,
        @Size(max = 20)             String cnpjCpf,
        @NotBlank @Email @Size(max = 200) String email,
        @NotNull                    TipoNegocio tipoNegocio,
        @Valid                      WhiteLabelRequest whiteLabel
) {}
