package br.com.seuprojeto.pascoa.order.domain.exception;

public class PedidoNotFoundException extends RuntimeException {
    public PedidoNotFoundException(Long id) { super("Pedido não encontrado: " + id); }
    public PedidoNotFoundException(String token) { super("Pedido não encontrado para token: " + token); }
}
