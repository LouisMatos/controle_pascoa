package br.com.seuprojeto.pascoa.notificacao.listener;

import br.com.seuprojeto.pascoa.notificacao.event.PedidoStatusEvent;
import br.com.seuprojeto.pascoa.notificacao.service.NotificacaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Escuta eventos de mudança de status de pedido e dispara notificações
 * de forma assíncrona (thread separada via @Async).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificacaoEventListener {

    private final NotificacaoService notificacaoService;

    @Async
    @EventListener
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
