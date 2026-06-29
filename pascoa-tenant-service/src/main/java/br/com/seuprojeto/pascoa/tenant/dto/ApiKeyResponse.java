package br.com.seuprojeto.pascoa.tenant.dto;

import br.com.seuprojeto.pascoa.tenant.domain.ApiKey;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ApiKeyResponse(
        Long id,
        String tenantId,
        String nome,
        /** Apenas o prefixo é seguro de exibir após criação ("ff_live_a1b2…"). */
        String chavePrefix,
        String escopos,
        boolean ativa,
        LocalDateTime criadaEm,
        LocalDateTime ultimaUsadaEm,
        LocalDate expiraEm,
        /** Chave plain-text completa — preenchida APENAS na resposta de criação. */
        String chavePlain
) {
    public static ApiKeyResponse from(ApiKey k, String plain) {
        return new ApiKeyResponse(k.getId(), k.getTenantId(), k.getNome(),
                k.getChavePrefix(), k.getEscopos(), k.isAtiva(),
                k.getCriadaEm(), k.getUltimaUsadaEm(), k.getExpiraEm(), plain);
    }

    public static ApiKeyResponse from(ApiKey k) {
        return from(k, null);
    }
}
