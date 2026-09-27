package br.com.seuprojeto.pascoa.notification.adapter.out.channel;

import br.com.seuprojeto.pascoa.notification.application.port.out.WhatsAppPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WhatsAppAdapter implements WhatsAppPort {

    @Value("${notification.whatsapp.enabled:false}")
    private boolean enabled;

    @Value("${notification.whatsapp.api-url:}")
    private String apiUrl;

    @Override
    public void enviar(String telefone, String mensagem) {
        if (!enabled || apiUrl.isBlank()) {
            throw new UnsupportedOperationException(
                    "WhatsApp não configurado. Defina notification.whatsapp.enabled=true e api-url.");
        }
        // Integração real com API de WhatsApp Business (ex: Meta, Twilio, Z-API)
        // Implementação específica depende do provedor escolhido
        log.info("[WhatsApp] Enviando para {}: {}", telefone, mensagem.substring(0, Math.min(50, mensagem.length())));
        // RestTemplate / WebClient call aqui
    }
}
