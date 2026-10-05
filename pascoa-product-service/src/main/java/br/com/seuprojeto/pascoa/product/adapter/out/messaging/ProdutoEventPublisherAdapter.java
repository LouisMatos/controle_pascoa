package br.com.seuprojeto.pascoa.product.adapter.out.messaging;

import br.com.seuprojeto.pascoa.product.application.port.out.ProdutoEventPublisherPort;
import br.com.seuprojeto.pascoa.product.domain.model.Produto;
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
public class ProdutoEventPublisherAdapter implements ProdutoEventPublisherPort {

    private static final String EXCHANGE = "pascoa.orders";

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishProdutoCriado(Produto produto) {
        publish("product.created", Map.of(
                "eventId", UUID.randomUUID().toString(),
                "eventType", "product.created",
                "productId", produto.getId(),
                "nome", produto.getNome(),
                "preco", produto.getPreco(),
                "categoria", produto.getCategoria().name(),
                "occurredOn", Instant.now().toString()
        ));
    }

    @Override
    public void publishProdutoAtualizado(Produto produto) {
        publish("product.updated", Map.of(
                "eventId", UUID.randomUUID().toString(),
                "eventType", "product.updated",
                "productId", produto.getId(),
                "preco", produto.getPreco(),
                "disponivel", produto.isDisponivel(),
                "occurredOn", Instant.now().toString()
        ));
    }

    @Override
    public void publishProdutoInativado(Long produtoId) {
        publish("product.deactivated", Map.of(
                "eventId", UUID.randomUUID().toString(),
                "eventType", "product.deactivated",
                "productId", produtoId,
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
