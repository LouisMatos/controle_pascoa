package br.com.seuprojeto.pascoa.product.adapter.out.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class InventoryEventConsumer {

    private final Set<String> processados = ConcurrentHashMap.newKeySet();

    @RabbitListener(queues = "#{T(br.com.seuprojeto.pascoa.product.config.RabbitConfig).QUEUE_STOCK_CRITICAL}")
    public void onEstoqueCritico(Map<String, Object> event) {
        String eventId = (String) event.get("eventId");
        if (eventId == null || !processados.add(eventId)) return;

        log.warn("Estoque crítico detectado: materiaPrima={}, quantidade={}",
                event.get("nome"), event.get("quantidadeAtual"));
        // Ponto de extensão: marcar produtos relacionados como indisponíveis
    }
}
