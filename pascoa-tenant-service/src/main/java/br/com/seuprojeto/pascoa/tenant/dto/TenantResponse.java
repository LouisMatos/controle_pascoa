package br.com.seuprojeto.pascoa.tenant.dto;

import br.com.seuprojeto.pascoa.tenant.domain.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TenantResponse(
        String id,
        String razaoSocial,
        String cnpjCpf,
        String email,
        PlanoAssinatura plano,
        TipoNegocio tipoNegocio,
        StatusTenant status,
        LocalDate dataExpiracao,
        String dominioCustom,
        LocalDateTime criadoEm
) {
    public static TenantResponse from(Tenant t) {
        return new TenantResponse(
                t.getId(),
                t.getRazaoSocial(),
                t.getCnpjCpf(),
                t.getEmail(),
                t.getPlano(),
                t.getTipoNegocio(),
                t.getStatus(),
                t.getDataExpiracao(),
                t.getDominioCustom(),
                t.getCriadoEm()
        );
    }
}
