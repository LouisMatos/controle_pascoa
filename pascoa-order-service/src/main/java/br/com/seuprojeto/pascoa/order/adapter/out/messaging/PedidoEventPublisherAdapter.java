package br.com.seuprojeto.pascoa.order.adapter.out.messaging;

import br.com.seuprojeto.pascoa.order.application.port.out.PedidoEventPublisherPort;
import br.com.seuprojeto.pascoa.order.domain.model.Pedido;
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
public class PedidoEventPublisherAdapter implements PedidoEventPublisherPort {

    private static final String EXCHANGE = "pascoa.orders";

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishPedidoCriado(Pedido p) {
        publish("order.created", baseEvent(p, "order.created"));
    }

    @Override
    public void publishPedidoConfirmado(Pedido p) {
        publish("order.confirmed", baseEvent(p, "order.confirmed"));
    }

    @Override
    public void publishPedidoCancelado(Pedido p) {
        publish("order.cancelled", baseEvent(p, "order.cancelled"));
    }

    @Override
    public void publishPedidoEntregue(Pedido p) {
        var payload = new java.util.HashMap<>(baseEvent(p, "order.delivered"));
        payload.put("total", p.total());
        payload.put("formaPagamento", p.getFormaPagamento() != null ? p.getFormaPagamento().name() : null);
        publish("order.delivered", payload);
    }

    private Map<String, Object> baseEvent(Pedido p, String eventType) {
        return Map.of(
                "eventId", UUID.randomUUID().toString(),
                "eventType", eventType,
                "orderId", p.getId(),
                "clienteId", p.getClienteId(),
                "status", p.getStatus().name(),
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
