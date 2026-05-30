package br.com.seuprojeto.pascoa.inventory.adapter.out.messaging;

import br.com.seuprojeto.pascoa.inventory.application.port.out.EstoqueEventPublisherPort;
import br.com.seuprojeto.pascoa.inventory.domain.model.MateriaPrima;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class EstoqueEventPublisherAdapter implements EstoqueEventPublisherPort {

    private static final String EXCHANGE = "pascoa.production";

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishEstoqueCritico(MateriaPrima mp) {
        publish("inventory.stock.critical", Map.of(
                "eventId", UUID.randomUUID().toString(),
                "eventType", "inventory.stock.critical",
                "materiaPrimaId", mp.getId(),
                "nome", mp.getNome(),
                "quantidadeAtual", mp.getQuantidadeEstoque(),
                "estoqueMinimo", mp.getEstoqueMinimo(),
                "occurredOn", Instant.now().toString()
        ));
    }

    @Override
    public void publishSaidaRealizada(Long materiaPrimaId, BigDecimal quantidade, Long ordemProducaoId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", UUID.randomUUID().toString());
        payload.put("eventType", "inventory.stock.exit");
        payload.put("materiaPrimaId", materiaPrimaId);
        payload.put("quantidade", quantidade);
        payload.put("ordemProducaoId", ordemProducaoId);
        payload.put("occurredOn", Instant.now().toString());
        publish("inventory.stock.exit", payload);
    }

    private void publish(String routingKey, Object payload) {
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, routingKey, payload);
        } catch (Exception e) {
            log.warn("Falha ao publicar evento '{}': {}", routingKey, e.getMessage());
        }
    }
}
