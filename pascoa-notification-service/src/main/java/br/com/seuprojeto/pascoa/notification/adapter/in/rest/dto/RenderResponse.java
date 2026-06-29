package br.com.seuprojeto.pascoa.notification.adapter.in.rest.dto;

public record RenderResponse(
        String tenantId,
        String evento,
        String canal,
        String assunto,
        String conteudo,
        /** true se o template encontrado é o global (sem tenantId específico). */
        boolean usandoFallbackGlobal
) {}
