package br.com.seuprojeto.pascoa.production.adapter.out.messaging;

import br.com.seuprojeto.pascoa.production.application.port.out.ProducaoEventPublisherPort;
import br.com.seuprojeto.pascoa.production.domain.model.OrdemProducao;
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
public class ProducaoEventPublisherAdapter implements ProducaoEventPublisherPort {

    private static final String EXCHANGE = "pascoa.production";

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishOrdemIniciada(OrdemProducao o) {
        publish("production.started", base(o, "production.started"));
    }

    @Override
    public void publishOrdemConcluida(OrdemProducao o) {
        var payload = new java.util.HashMap<>(base(o, "production.completed"));
        payload.put("pedidoId", o.getPedidoId());
        publish("production.completed", payload);
    }

    @Override
    public void publishOrdemCancelada(OrdemProducao o) {
        publish("production.cancelled", base(o, "production.cancelled"));
    }

    private Map<String, Object> base(OrdemProducao o, String type) {
        return Map.of(
                "eventId", UUID.randomUUID().toString(),
                "eventType", type,
                "ordemId", o.getId(),
                "pedidoId", o.getPedidoId(),
                "status", o.getStatus().name(),
                "occurredOn", Instant.now().toString()
        );
    }

    private void publish(String routingKey, Object payload) {
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, routingKey, payload);
        } catch (Exception e) {
            log.warn("Falha ao publicar evento '{}': {}", routingKey, e.getMessage());
        }
    }
}
