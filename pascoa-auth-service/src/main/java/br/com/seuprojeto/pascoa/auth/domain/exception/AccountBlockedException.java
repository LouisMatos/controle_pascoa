package br.com.seuprojeto.pascoa.auth.domain.exception;

public class AccountBlockedException extends RuntimeException {
    public AccountBlockedException() {
        super("Conta bloqueada por excesso de tentativas. Contate o administrador.");
    }
}
