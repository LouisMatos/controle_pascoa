package br.com.seuprojeto.pascoa.notificacao.listener;

import br.com.seuprojeto.pascoa.notificacao.event.PedidoStatusEvent;
import br.com.seuprojeto.pascoa.notificacao.service.NotificacaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Escuta eventos de mudança de status de pedido e dispara notificações
 * de forma assíncrona (thread separada via @Async).
 *
 * AFTER_COMMIT: garante que o evento só dispara após o commit da transação
 * publicadora. Em testes @Transactional (que fazem rollback) o listener
 * nunca é invocado, evitando acesso concorrente à Hibernate Session.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificacaoEventListener {

    private final NotificacaoService notificacaoService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPedidoStatus(PedidoStatusEvent event) {
        log.info("[NOTIF] Processando evento {} para pedido #{}", event.evento(), event.pedido().getId());
        try {
            notificacaoService.processar(event.pedido(), event.evento());
        } catch (Exception e) {
            log.error("[NOTIF] Erro ao processar evento {} para pedido #{}: {}",
                    event.evento(), event.pedido().getId(), e.getMessage(), e);
        }
    }
}
