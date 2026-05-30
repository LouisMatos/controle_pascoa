package br.com.seuprojeto.pascoa.product.domain.exception;

import java.math.BigDecimal;

public class PrecoInvalidoException extends RuntimeException {
    public PrecoInvalidoException(BigDecimal preco) {
        super("Preço inválido: " + preco + ". O preço deve ser maior que zero.");
    }
}
