package br.com.seuprojeto.pascoa.notification.adapter.out.messaging;

import br.com.seuprojeto.pascoa.notification.application.port.in.NotificacaoUseCase;
import br.com.seuprojeto.pascoa.notification.domain.model.Canal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class AllEventsConsumer {

    private final NotificacaoUseCase notificacaoUseCase;
    private final Set<String> processados = ConcurrentHashMap.newKeySet();

    @RabbitListener(queues = "#{T(br.com.seuprojeto.pascoa.notification.config.RabbitConfig).QUEUE_NOTIFICATIONS}")
    public void onEvent(Map<String, Object> event) {
        String eventId = (String) event.get("eventId");
        if (eventId == null || !processados.add(eventId)) return;

        String eventType = (String) event.getOrDefault("eventType", "unknown");
        log.debug("Evento recebido: type={}", eventType);

        try {
            NotificationPayload payload = buildPayload(eventType, event);
            if (payload == null) return; // evento sem destinatário conhecido

            notificacaoUseCase.enviar(new NotificacaoUseCase.EnviarCommand(
                    payload.destinatario(),
                    payload.canal(),
                    payload.assunto(),
                    payload.template(),
                    payload.variaveis(),
                    eventType,
                    String.valueOf(event.getOrDefault("orderId",
                            event.getOrDefault("customerId",
                            event.getOrDefault("ordemId", ""))))
            ));
        } catch (Exception e) {
            log.error("Erro ao processar evento {} para notificação: {}", eventType, e.getMessage());
        }
    }

    private NotificationPayload buildPayload(String eventType, Map<String, Object> event) {
        Map<String, String> vars = new HashMap<>();
        event.forEach((k, v) -> vars.put(k, String.valueOf(v)));

        return switch (eventType) {
            case "order.confirmed" -> new NotificationPayload(
                    resolveEmail(event),
                    Canal.EMAIL,
                    "Pedido Confirmado! 🎁",
                    "Olá! Seu pedido #{orderId} foi confirmado. Previsão de entrega: em breve. Acompanhe: /acompanhamento/{token}",
                    vars);

            case "order.delivered" -> new NotificationPayload(
                    resolveEmail(event),
                    Canal.EMAIL,
                    "Seu pedido foi entregue!",
                    "Seu pedido #{orderId} foi entregue. Obrigado pela preferência! 🍫",
                    vars);

            case "order.cancelled" -> new NotificationPayload(
                    resolveEmail(event),
                    Canal.EMAIL,
                    "Pedido Cancelado",
                    "Seu pedido #{orderId} foi cancelado. Entre em contato se tiver dúvidas.",
                    vars);

            case "auth.login.success" -> null; // sem notificação ao cliente

            default -> {
                log.debug("Evento {} sem template configurado.", eventType);
                yield null;
            }
        };
    }

    private String resolveEmail(Map<String, Object> event) {
        Object email = event.get("clienteEmail");
        return email != null ? String.valueOf(email) : "noreply@pascoa.com.br";
    }

    private record NotificationPayload(
            String destinatario,
            Canal canal,
            String assunto,
            String template,
            Map<String, String> variaveis
    ) {}
}
