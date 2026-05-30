package br.com.seuprojeto.pascoa.order.application.port.out;

import java.util.Optional;

public interface ClienteServicePort {
    record ClienteInfo(Long id, String nome, String email) {}
    Optional<ClienteInfo> findById(Long clienteId);
}
