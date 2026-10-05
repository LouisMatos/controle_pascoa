package br.com.seuprojeto.pascoa.notification.adapter.out.channel;

import br.com.seuprojeto.pascoa.notification.application.port.out.SmsPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SmsAdapter implements SmsPort {

    @Value("${notification.sms.enabled:false}")
    private boolean enabled;

    @Override
    public void enviar(String telefone, String mensagem) {
        if (!enabled) {
            log.warn("[SMS] Canal desabilitado. Mensagem para {}: {}", telefone,
                    mensagem.substring(0, Math.min(80, mensagem.length())));
            return;
        }
        // Integração real com provedor SMS (Twilio, Zenvia, etc.)
        log.info("[SMS] Enviando para {}", telefone);
    }
}
