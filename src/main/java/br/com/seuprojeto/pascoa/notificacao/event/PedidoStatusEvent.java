package br.com.seuprojeto.pascoa.notificacao.event;

import br.com.seuprojeto.pascoa.notificacao.entity.EventoNotificacao;
import br.com.seuprojeto.pascoa.pedido.entity.Pedido;

/**
 * Evento publicado no contexto Spring sempre que um pedido muda de status
 * de forma relevante para notificações.
 */
public record PedidoStatusEvent(Pedido pedido, EventoNotificacao evento) {}
