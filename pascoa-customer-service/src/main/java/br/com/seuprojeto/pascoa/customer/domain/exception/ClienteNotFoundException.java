package br.com.seuprojeto.pascoa.customer.domain.exception;

public class ClienteNotFoundException extends RuntimeException {
    public ClienteNotFoundException(Long id) {
        super("Cliente não encontrado: " + id);
    }
}
