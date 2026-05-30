package br.com.seuprojeto.pascoa.order.application.port.in;

import br.com.seuprojeto.pascoa.order.domain.model.FormaPagamento;
import br.com.seuprojeto.pascoa.order.domain.model.Pedido;

import java.math.BigDecimal;
import java.util.List;

public interface PedidoUseCase {

    record CriarPedidoCommand(Long clienteId, String observacao) {}

    record AdicionarItemCommand(Long pedidoId, Long produtoId, int quantidade) {}

    record RegistrarPagamentoCommand(Long pedidoId, FormaPagamento forma, BigDecimal valor) {}

    Pedido criar(CriarPedidoCommand command);

    Pedido buscarPorId(Long id);

    Pedido buscarPorToken(String token);

    List<Pedido> listar();

    Pedido adicionarItem(AdicionarItemCommand command);

    Pedido removerItem(Long pedidoId, Long itemId);

    Pedido confirmar(Long pedidoId);

    Pedido cancelar(Long pedidoId);

    Pedido marcarPronto(Long pedidoId);

    Pedido registrarEntrega(Long pedidoId);

    Pedido registrarPagamento(RegistrarPagamentoCommand command);
}
