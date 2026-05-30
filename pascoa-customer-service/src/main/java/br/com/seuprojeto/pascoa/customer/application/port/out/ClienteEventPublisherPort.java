package br.com.seuprojeto.pascoa.customer.application.port.out;

import br.com.seuprojeto.pascoa.customer.domain.model.Cliente;

public interface ClienteEventPublisherPort {
    void publishClienteCriado(Cliente cliente);
    void publishClienteAtualizado(Cliente cliente);
    void publishClienteInativado(Long clienteId);
}
