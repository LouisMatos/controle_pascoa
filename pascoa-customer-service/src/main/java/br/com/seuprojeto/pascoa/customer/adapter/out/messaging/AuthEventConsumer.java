package br.com.seuprojeto.pascoa.customer.adapter.out.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class AuthEventConsumer {

    // Idempotência: guarda eventIds já processados (em produção usar Redis/BD)
    private final Set<String> processados = ConcurrentHashMap.newKeySet();

    @RabbitListener(queues = "#{T(br.com.seuprojeto.pascoa.customer.config.RabbitConfig).QUEUE_AUTH_LOGIN}")
    public void onLoginSuccess(Map<String, Object> event) {
        String eventId = (String) event.get("eventId");
        if (eventId == null || !processados.add(eventId)) return;

        log.debug("Login detectado para userId={}", event.get("userId"));
        // Ponto de extensão: registrar último acesso, atualizar score de atividade, etc.
    }
}
