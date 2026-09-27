package br.com.seuprojeto.pascoa.auth.domain.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Credenciais inválidas.");
    }
}
