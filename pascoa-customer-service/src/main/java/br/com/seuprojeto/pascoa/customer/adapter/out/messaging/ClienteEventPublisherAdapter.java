package br.com.seuprojeto.pascoa.customer.adapter.out.messaging;

import br.com.seuprojeto.pascoa.customer.application.port.out.ClienteEventPublisherPort;
import br.com.seuprojeto.pascoa.customer.domain.model.Cliente;
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
public class ClienteEventPublisherAdapter implements ClienteEventPublisherPort {

    private static final String EXCHANGE = "pascoa.customers";

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishClienteCriado(Cliente cliente) {
        publish("customer.created", Map.of(
                "eventId", UUID.randomUUID().toString(),
                "eventType", "customer.created",
                "customerId", cliente.getId(),
                "nome", cliente.getNome(),
                "email", cliente.getEmail(),
                "occurredOn", Instant.now().toString()
        ));
    }

    @Override
    public void publishClienteAtualizado(Cliente cliente) {
        publish("customer.updated", Map.of(
                "eventId", UUID.randomUUID().toString(),
                "eventType", "customer.updated",
                "customerId", cliente.getId(),
                "occurredOn", Instant.now().toString()
        ));
    }

    @Override
    public void publishClienteInativado(Long clienteId) {
        publish("customer.deactivated", Map.of(
                "eventId", UUID.randomUUID().toString(),
                "eventType", "customer.deactivated",
                "customerId", clienteId,
                "occurredOn", Instant.now().toString()
        ));
    }

    private void publish(String routingKey, Object payload) {
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, routingKey, payload);
        } catch (Exception e) {
            log.warn("Falha ao publicar evento '{}': {}", routingKey, e.getMessage());
        }
    }
}
