package br.com.seuprojeto.pascoa.order.application.port.out;

import br.com.seuprojeto.pascoa.order.domain.model.Pedido;

public interface PedidoEventPublisherPort {
    void publishPedidoCriado(Pedido pedido);
    void publishPedidoConfirmado(Pedido pedido);
    void publishPedidoCancelado(Pedido pedido);
    void publishPedidoEntregue(Pedido pedido);
}
