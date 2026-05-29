package br.com.seuprojeto.pascoa.notificacao.service;

import br.com.seuprojeto.pascoa.notificacao.entity.ConfiguracaoCanal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Item 25 — SMS fallback.
 *
 * <p>Envia SMS via webhook HTTP genérico configurável em {@code configuracao_canal} (tipo = SMS).
 * Quando {@code testMode = true} (padrão) ou a URL não está configurada, apenas loga o envio.
 *
 * <p>Formato do payload HTTP POST enviado ao provedor:
 * <pre>{ "to": "+5511999990000", "message": "..." }</pre>
 *
 * <p>A autenticação usa {@code api_key} no header {@code Authorization: Bearer <api_key>}.
 * Compatível com Twilio Notify, Zenvia, AWS SNS HTTP wrapper, etc.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SmsService {

    private final RestTemplate restTemplate;

    /**
     * Envia SMS ao número informado usando a configuração do canal.
     *
     * @param config  configuração do canal SMS (url, api_key, testMode)
     * @param numero  número no formato nacional ou internacional (ex: 11999990000 ou +5511999990000)
     * @param mensagem texto da mensagem
     */
    public void enviar(ConfiguracaoCanal config, String numero, String mensagem) {
        String numeroFormatado = formatarNumero(numero);

        if (Boolean.TRUE.equals(config.getTestMode())
                || config.getApiUrl() == null
                || config.getApiUrl().isBlank()) {
            log.info("[SMS-TEST] Para: {} | Mensagem: {}", numeroFormatado, mensagem);
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (config.getApiKey() != null && !config.getApiKey().isBlank()) {
                headers.setBearerAuth(config.getApiKey());
            }

            Map<String, String> payload = Map.of(
                    "to",      numeroFormatado,
                    "message", mensagem
            );

            HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, headers);
            restTemplate.postForEntity(config.getApiUrl(), request, String.class);
            log.info("[SMS] Enviado para {} via {}", numeroFormatado, config.getApiUrl());

        } catch (Exception e) {
            log.error("[SMS] Falha ao enviar para {}: {}", numeroFormatado, e.getMessage());
            throw e;
        }
    }

    /**
     * Normaliza o número para formato internacional +55XXXXXXXXXXX.
     * Aceita dígitos puros, com traços, parênteses ou já no formato +55.
     */
    public String formatarNumero(String numero) {
        if (numero == null) return "";
        String digits = numero.replaceAll("[^0-9+]", "");
        if (digits.startsWith("+")) return digits;
        if (digits.startsWith("55") && digits.length() >= 12) return "+" + digits;
        return "+55" + digits;
    }
}
