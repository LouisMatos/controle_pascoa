package br.com.seuprojeto.pascoa.production.adapter.out.messaging;

import br.com.seuprojeto.pascoa.production.application.port.in.ProducaoUseCase;
import br.com.seuprojeto.pascoa.production.domain.model.ItemOrdem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final ProducaoUseCase producaoUseCase;
    private final Set<String> processados = ConcurrentHashMap.newKeySet();

    @RabbitListener(queues = "#{T(br.com.seuprojeto.pascoa.production.config.RabbitConfig).QUEUE_ORDER_CONFIRMED}")
    public void onPedidoConfirmado(Map<String, Object> event) {
        String eventId = (String) event.get("eventId");
        if (eventId == null || !processados.add(eventId)) return;

        Long pedidoId = toLong(event.get("orderId"));
        String nomeCliente = (String) event.getOrDefault("nomeCliente", "Cliente");

        // Itens vindos do evento (opcional — cria ordem mínima se ausente)
        List<ItemOrdem> itens = parseItens(event);

        try {
            producaoUseCase.criar(new ProducaoUseCase.CriarOrdemCommand(
                    pedidoId, nomeCliente, itens, LocalDate.now().plusDays(3)));
            log.info("Ordem de produção criada para pedido {}", pedidoId);
        } catch (Exception e) {
            log.error("Erro ao criar ordem de produção para pedido {}: {}", pedidoId, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<ItemOrdem> parseItens(Map<String, Object> event) {
        Object rawItens = event.get("itens");
        if (rawItens instanceof List<?> list && !list.isEmpty()) {
            return list.stream()
                    .filter(i -> i instanceof Map)
                    .map(i -> {
                        Map<String, Object> m = (Map<String, Object>) i;
                        return ItemOrdem.builder()
                                .produtoId(toLong(m.get("produtoId")))
                                .nomeProduto((String) m.getOrDefault("nomeProduto", "Produto"))
                                .quantidade(toInt(m.get("quantidade")))
                                .build();
                    }).toList();
        }
        return List.of();
    }

    private Long toLong(Object v) {
        if (v instanceof Integer i) return i.longValue();
        if (v instanceof Long l) return l;
        if (v != null) return Long.parseLong(String.valueOf(v));
        return 0L;
    }

    private int toInt(Object v) {
        if (v instanceof Integer i) return i;
        if (v instanceof Long l) return l.intValue();
        return 1;
    }
}
