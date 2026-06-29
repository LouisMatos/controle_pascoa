package br.com.seuprojeto.pascoa.notification.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Map;

public record RenderRequest(
        String tenantId,
        @NotBlank String evento,
        @NotBlank @Pattern(regexp = "EMAIL|WHATSAPP|SMS") String canal,
        Map<String, String> variaveis
) {}
