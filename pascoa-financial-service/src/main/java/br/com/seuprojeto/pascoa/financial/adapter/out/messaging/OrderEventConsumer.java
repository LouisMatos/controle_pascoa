package br.com.seuprojeto.pascoa.financial.adapter.out.messaging;

import br.com.seuprojeto.pascoa.financial.application.port.in.FinanceiroUseCase;
import br.com.seuprojeto.pascoa.financial.application.port.out.LancamentoRepositoryPort;
import br.com.seuprojeto.pascoa.financial.domain.model.CategoriaLancamento;
import br.com.seuprojeto.pascoa.financial.domain.model.TipoLancamento;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final FinanceiroUseCase financeiroUseCase;
    private final LancamentoRepositoryPort lancamentoRepository;
    private final Set<String> processados = ConcurrentHashMap.newKeySet();

    @RabbitListener(queues = "#{T(br.com.seuprojeto.pascoa.financial.config.RabbitConfig).QUEUE_ORDER_DELIVERED}")
    public void onPedidoEntregue(Map<String, Object> event) {
        String eventId = (String) event.get("eventId");
        if (eventId == null || !processados.add(eventId)) return;

        String orderId = String.valueOf(event.get("orderId"));
        String origem = "order-service";

        // Idempotência via banco
        if (lancamentoRepository.existsByReferenciaIdAndOrigem(orderId, origem)) {
            log.debug("Lançamento já registrado para pedido {}", orderId);
            return;
        }

        Object totalObj = event.get("total");
        if (totalObj == null) return;

        BigDecimal total = new BigDecimal(String.valueOf(totalObj));
        try {
            financeiroUseCase.registrar(new FinanceiroUseCase.RegistrarLancamentoCommand(
                    TipoLancamento.RECEITA,
                    CategoriaLancamento.VENDA,
                    "Venda — Pedido #" + orderId,
                    total,
                    LocalDate.now(),
                    orderId
            ));
            log.info("Receita de R$ {} registrada para pedido {}", total, orderId);
        } catch (Exception e) {
            log.error("Erro ao registrar receita do pedido {}: {}", orderId, e.getMessage());
        }
    }
}
