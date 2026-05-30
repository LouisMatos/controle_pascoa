package br.com.seuprojeto.pascoa.inventory.adapter.out.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class OrderEventConsumer {

    // Idempotência em memória — em produção usar Redis ou tabela de eventos processados
    private final Set<String> processados = ConcurrentHashMap.newKeySet();

    @RabbitListener(queues = "#{T(br.com.seuprojeto.pascoa.inventory.config.RabbitConfig).QUEUE_ORDER_CONFIRMED}")
    public void onOrderConfirmed(Map<String, Object> event) {
        String eventId = (String) event.get("eventId");
        if (eventId == null || !processados.add(eventId)) return;

        log.info("Pedido confirmado recebido: orderId={}", event.get("orderId"));
        // Ponto de extensão: reservar insumos automaticamente a partir da ficha técnica
    }
}
