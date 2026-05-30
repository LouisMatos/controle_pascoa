package br.com.seuprojeto.pascoa.notificacao.service;

import br.com.seuprojeto.pascoa.notificacao.dto.ResultadoConexaoDto;
import br.com.seuprojeto.pascoa.notificacao.entity.ConfiguracaoCanal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Integração com Evolution API (WhatsApp self-hosted).
 *
 * Endpoints utilizados:
 *   GET  {apiUrl}/instance/connectionState/{instancia}  → verifica conexão
 *   POST {apiUrl}/message/sendText/{instancia}          → envia mensagem
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppService {

    private final RestTemplate restTemplate;

    // -----------------------------------------------------------------------
    // Envio de mensagem
    // -----------------------------------------------------------------------

    /**
     * Envia mensagem de texto via Evolution API.
     * Quando {@code testMode=true}, apenas registra no log (não envia de verdade).
     *
     * @param config   configuração do canal (apiUrl, apiKey, remetente = nome da instância)
     * @param numero   número destino — somente dígitos ou formato (11)99999-9999
     * @param mensagem texto a enviar
     */
    public void enviar(ConfiguracaoCanal config, String numero, String mensagem) {
        if (Boolean.TRUE.equals(config.getTestMode())) {
            log.info("[WHATSAPP TEST MODE] Para: {} | Mensagem: {}", numero, mensagem);
            return;
        }
        enviarReal(config, numero, mensagem);
    }

    /**
     * Envia mensagem de teste REAL ignorando testMode — usado exclusivamente
     * pelo fluxo de validação do Item 5 para confirmar que a API está acessível.
     */
    public void enviarMensagemDeTeste(ConfiguracaoCanal config, String numero, String mensagem) {
        log.info("[WHATSAPP TESTE MANUAL] Para: {} | Mensagem: {}", numero, mensagem);
        enviarReal(config, numero, mensagem);
    }

    private void enviarReal(ConfiguracaoCanal config, String numero, String mensagem) {
        String url = config.getApiUrl() + "/message/sendText/" + config.getRemetente();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", config.getApiKey());

        Map<String, Object> body = Map.of(
            "number", formatarNumero(numero),
            "text", mensagem
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            log.info("[WHATSAPP] Mensagem enviada para {} — HTTP {}", numero, response.getStatusCode());
        } catch (Exception e) {
            log.error("[WHATSAPP] Falha ao enviar para {}: {}", numero, e.getMessage());
            throw new RuntimeException("Falha ao enviar WhatsApp para " + numero + ": " + e.getMessage(), e);
        }
    }

    // -----------------------------------------------------------------------
    // Verificação de conectividade
    // -----------------------------------------------------------------------

    /**
     * Verifica o estado da instância WhatsApp na Evolution API.
     * Retorna um {@link ResultadoConexaoDto} com o estado reportado pela API
     * (open = conectado, close/connecting = desconectado/pendente).
     */
    @SuppressWarnings("unchecked")
    public ResultadoConexaoDto verificarConexao(ConfiguracaoCanal config) {
        if (config.getApiUrl() == null || config.getApiUrl().isBlank()) {
            return ResultadoConexaoDto.falha("URL da API não configurada.");
        }
        if (config.getRemetente() == null || config.getRemetente().isBlank()) {
            return ResultadoConexaoDto.falha("Nome da instância (Remetente) não configurado.");
        }

        String url = config.getApiUrl() + "/instance/connectionState/" + config.getRemetente();

        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", config.getApiKey() != null ? config.getApiKey() : "");

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
            Map<String, Object> body = response.getBody();

            if (body == null) {
                return ResultadoConexaoDto.falha("A API retornou resposta vazia.");
            }

            // Evolution API retorna: { "instance": { "instanceName": "...", "state": "open" } }
            Object instanceObj = body.get("instance");
            String estado = "desconhecido";
            if (instanceObj instanceof Map<?, ?> instanceMap) {
                Object stateVal = instanceMap.get("state");
                if (stateVal != null) estado = stateVal.toString();
            }

            boolean conectado = "open".equalsIgnoreCase(estado);
            String mensagem = conectado
                ? "Instância conectada (state: " + estado + ")"
                : "Instância não está aberta (state: " + estado + "). Escaneie o QR Code no painel da Evolution API.";

            log.info("[WHATSAPP] Verificação de conexão — instância: {} | estado: {}", config.getRemetente(), estado);
            return new ResultadoConexaoDto(conectado, estado, mensagem);

        } catch (Exception e) {
            String msg = "Não foi possível conectar à Evolution API: " + e.getMessage();
            log.warn("[WHATSAPP] {}", msg);
            return ResultadoConexaoDto.falha(msg);
        }
    }

    // -----------------------------------------------------------------------
    // Utilitários
    // -----------------------------------------------------------------------

    /**
     * Remove caracteres não-numéricos e adiciona DDI 55 (Brasil) se o número
     * tiver 10 ou 11 dígitos.
     */
    public String formatarNumero(String numero) {
        String apenas = numero.replaceAll("[^0-9]", "");
        if (apenas.length() == 10 || apenas.length() == 11) {
            return "55" + apenas;
        }
        return apenas;
    }
}
