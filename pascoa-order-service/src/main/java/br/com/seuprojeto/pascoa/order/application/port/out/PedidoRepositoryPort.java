package br.com.seuprojeto.pascoa.order.application.port.out;

import br.com.seuprojeto.pascoa.order.domain.model.Pedido;

import java.util.List;
import java.util.Optional;

public interface PedidoRepositoryPort {
    Optional<Pedido> findById(Long id);
    Optional<Pedido> findByToken(String token);
    List<Pedido> findAll();
    Pedido save(Pedido pedido);
}
