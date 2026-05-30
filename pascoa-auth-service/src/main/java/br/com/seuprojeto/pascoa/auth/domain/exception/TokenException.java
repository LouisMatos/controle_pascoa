package br.com.seuprojeto.pascoa.auth.domain.exception;

public class TokenException extends RuntimeException {
    public TokenException(String message) {
        super(message);
    }

    public static TokenException invalid() {
        return new TokenException("Token inválido ou malformado.");
    }

    public static TokenException expired() {
        return new TokenException("Token expirado.");
    }

    public static TokenException blacklisted() {
        return new TokenException("Token revogado.");
    }
}
