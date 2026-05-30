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
public class ProductionEventConsumer {

    private final FinanceiroUseCase financeiroUseCase;
    private final LancamentoRepositoryPort lancamentoRepository;
    private final Set<String> processados = ConcurrentHashMap.newKeySet();

    @RabbitListener(queues = "#{T(br.com.seuprojeto.pascoa.financial.config.RabbitConfig).QUEUE_PRODUCTION_COMPLETED}")
    public void onProducaoConcluida(Map<String, Object> event) {
        String eventId = (String) event.get("eventId");
        if (eventId == null || !processados.add(eventId)) return;

        String ordemId = String.valueOf(event.get("ordemId"));
        String origem = "production-service";

        if (lancamentoRepository.existsByReferenciaIdAndOrigem(ordemId, origem)) return;

        // Registra custo fixo simbólico de produção (mão de obra estimada)
        try {
            financeiroUseCase.registrar(new FinanceiroUseCase.RegistrarLancamentoCommand(
                    TipoLancamento.DESPESA,
                    CategoriaLancamento.PRODUCAO,
                    "Custo de produção — Ordem #" + ordemId,
                    BigDecimal.ZERO, // valor real virá de ficha técnica futuramente
                    LocalDate.now(),
                    ordemId
            ));
        } catch (Exception e) {
            log.error("Erro ao registrar custo de produção da ordem {}: {}", ordemId, e.getMessage());
        }
    }
}
