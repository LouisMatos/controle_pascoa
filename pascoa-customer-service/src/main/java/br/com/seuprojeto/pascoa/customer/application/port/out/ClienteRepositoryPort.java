package br.com.seuprojeto.pascoa.customer.application.port.out;

import br.com.seuprojeto.pascoa.customer.domain.model.Cliente;

import java.util.List;
import java.util.Optional;

public interface ClienteRepositoryPort {
    Optional<Cliente> findById(Long id);
    Optional<Cliente> findByEmail(String email);
    List<Cliente> findAllAtivos();
    Cliente save(Cliente cliente);
    boolean existsByEmail(String email);
}
