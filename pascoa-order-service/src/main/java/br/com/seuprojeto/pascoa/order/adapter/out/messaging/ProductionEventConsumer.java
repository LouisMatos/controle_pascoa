package br.com.seuprojeto.pascoa.order.adapter.out.messaging;

import br.com.seuprojeto.pascoa.order.application.port.in.PedidoUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductionEventConsumer {

    private final PedidoUseCase pedidoUseCase;
    private final Set<String> processados = ConcurrentHashMap.newKeySet();

    @RabbitListener(queues = "#{T(br.com.seuprojeto.pascoa.order.config.RabbitConfig).QUEUE_PRODUCTION_COMPLETED}")
    public void onProducaoConcluida(Map<String, Object> event) {
        String eventId = (String) event.get("eventId");
        if (eventId == null || !processados.add(eventId)) return;

        Object pedidoIdObj = event.get("pedidoId");
        if (pedidoIdObj == null) return;

        Long pedidoId = pedidoIdObj instanceof Integer i ? i.longValue() : (Long) pedidoIdObj;
        try {
            pedidoUseCase.marcarPronto(pedidoId);
            log.info("Pedido {} marcado como PRONTO após produção.", pedidoId);
        } catch (Exception e) {
            log.error("Erro ao marcar pedido {} como pronto: {}", pedidoId, e.getMessage());
        }
    }
}
