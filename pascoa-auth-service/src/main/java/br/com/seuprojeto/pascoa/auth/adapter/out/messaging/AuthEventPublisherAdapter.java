package br.com.seuprojeto.pascoa.auth.adapter.out.messaging;

import br.com.seuprojeto.pascoa.auth.application.port.out.AuthEventPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthEventPublisherAdapter implements AuthEventPublisherPort {

    private static final String EXCHANGE = "pascoa.customers";

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishLoginSuccess(Long userId, String login) {
        Map<String, Object> event = Map.of(
                "eventId", UUID.randomUUID().toString(),
                "eventType", "auth.login.success",
                "userId", userId,
                "login", login,
                "occurredOn", Instant.now().toString()
        );
        publish("auth.login.success", event);
    }

    @Override
    public void publishLoginFailed(String login, String reason) {
        Map<String, Object> event = Map.of(
                "eventId", UUID.randomUUID().toString(),
                "eventType", "auth.login.failed",
                "login", login,
                "reason", reason,
                "occurredOn", Instant.now().toString()
        );
        publish("auth.login.failed", event);
    }

    private void publish(String routingKey, Object payload) {
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, routingKey, payload);
        } catch (Exception e) {
            log.warn("Falha ao publicar evento '{}': {}", routingKey, e.getMessage());
        }
    }
}
