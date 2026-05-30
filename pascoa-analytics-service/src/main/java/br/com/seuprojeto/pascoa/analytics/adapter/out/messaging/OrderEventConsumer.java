package br.com.seuprojeto.pascoa.analytics.adapter.out.messaging;

import br.com.seuprojeto.pascoa.analytics.application.port.out.RegistroVendaRepositoryPort;
import br.com.seuprojeto.pascoa.analytics.domain.model.RegistroVenda;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final RegistroVendaRepositoryPort repository;
    private final Set<String> processados = ConcurrentHashMap.newKeySet();

    @SuppressWarnings("unchecked")
    @RabbitListener(queues = "#{T(br.com.seuprojeto.pascoa.analytics.config.RabbitConfig).QUEUE_ORDER_DELIVERED}")
    @Transactional
    public void onPedidoEntregue(Map<String, Object> event) {
        String eventId = (String) event.get("eventId");
        if (eventId == null || !processados.add(eventId)) return;

        Long pedidoId  = toLong(event.get("orderId"));
        Long clienteId = toLong(event.get("clienteId"));

        // Idempotência via banco: não registrar duas vezes o mesmo pedido
        if (repository.existsByPedidoId(pedidoId)) {
            log.debug("Pedido {} já registrado no analytics.", pedidoId);
            return;
        }

        LocalDate hoje = LocalDate.now();
        Object totalObj = event.get("total");
        BigDecimal total = totalObj != null ? new BigDecimal(String.valueOf(totalObj)) : BigDecimal.ZERO;

        // Registra itens do pedido, ou um registro agregado se itens não vierem no evento
        Object rawItens = event.get("itens");
        if (rawItens instanceof List<?> itens && !itens.isEmpty()) {
            itens.stream()
                    .filter(i -> i instanceof Map)
                    .forEach(i -> {
                        Map<String, Object> item = (Map<String, Object>) i;
                        repository.save(RegistroVenda.builder()
                                .pedidoId(pedidoId)
                                .clienteId(clienteId)
                                .produtoId(toLong(item.get("produtoId")))
                                .nomeProduto(String.valueOf(item.getOrDefault("nomeProduto", "Produto")))
                                .quantidade(toInt(item.get("quantidade")))
                                .valorTotal(new BigDecimal(String.valueOf(
                                        item.getOrDefault("subtotal", "0"))))
                                .dataVenda(hoje)
                                .ano(hoje.getYear())
                                .mes(hoje.getMonthValue())
                                .build());
                    });
        } else {
            // Registro aggregado sem breakdown por produto
            repository.save(RegistroVenda.builder()
                    .pedidoId(pedidoId).clienteId(clienteId)
                    .produtoId(0L).nomeProduto("N/A")
                    .quantidade(1).valorTotal(total)
                    .dataVenda(hoje).ano(hoje.getYear()).mes(hoje.getMonthValue())
                    .build());
        }
        log.info("Analytics: pedido {} registrado.", pedidoId);
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
