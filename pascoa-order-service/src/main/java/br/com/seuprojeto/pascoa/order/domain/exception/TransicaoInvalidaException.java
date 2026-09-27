package br.com.seuprojeto.pascoa.order.domain.exception;

import br.com.seuprojeto.pascoa.order.domain.model.StatusPedido;

public class TransicaoInvalidaException extends RuntimeException {
    public TransicaoInvalidaException(Long pedidoId, StatusPedido atual, StatusPedido destino) {
        super(String.format("Pedido %d: transição inválida de %s para %s.", pedidoId, atual, destino));
    }
}
