package br.com.seuprojeto.pascoa.tenant.dto;

import br.com.seuprojeto.pascoa.tenant.domain.PlanoAssinatura;
import br.com.seuprojeto.pascoa.tenant.domain.TipoNegocio;
import jakarta.validation.constraints.*;

public record RegistrarTenantRequest(
        @NotBlank
        @Pattern(regexp = "[a-z0-9-]{3,50}",
                message = "id deve conter apenas letras minúsculas, números e hífens (3-50 chars)")
        String id,

        @NotBlank @Size(max = 200)
        String razaoSocial,

        @Size(max = 20)
        String cnpjCpf,

        @NotBlank @Email @Size(max = 200)
        String email,

        @NotNull
        TipoNegocio tipoNegocio,

        PlanoAssinatura plano
) {}
