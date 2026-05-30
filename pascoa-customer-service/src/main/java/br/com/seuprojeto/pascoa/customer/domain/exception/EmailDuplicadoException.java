package br.com.seuprojeto.pascoa.customer.domain.exception;

public class EmailDuplicadoException extends RuntimeException {
    public EmailDuplicadoException(String email) {
        super("E-mail já cadastrado: " + email);
    }
}
