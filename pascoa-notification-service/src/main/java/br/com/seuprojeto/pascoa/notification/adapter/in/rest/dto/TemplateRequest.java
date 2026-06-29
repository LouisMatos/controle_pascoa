package br.com.seuprojeto.pascoa.notification.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TemplateRequest(
        /** {@code null} para template global (admin da plataforma). */
        @Size(max = 50)              String tenantId,
        @NotBlank @Size(max = 50)    String evento,
        @NotBlank @Pattern(regexp = "EMAIL|WHATSAPP|SMS")  String canal,
        @Size(max = 300)             String assunto,
        @NotBlank                    String conteudo
) {}
