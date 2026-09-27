package br.com.seuprojeto.pascoa.production.domain.exception;

import br.com.seuprojeto.pascoa.production.domain.model.StatusOrdem;

public class TransicaoOrdemInvalidaException extends RuntimeException {
    public TransicaoOrdemInvalidaException(Long id, StatusOrdem atual, StatusOrdem destino) {
        super(String.format("Ordem %d: transição inválida de %s para %s.", id, atual, destino));
    }
}
