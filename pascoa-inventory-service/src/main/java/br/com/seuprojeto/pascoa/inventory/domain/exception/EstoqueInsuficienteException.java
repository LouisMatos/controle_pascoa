package br.com.seuprojeto.pascoa.inventory.domain.exception;

import java.math.BigDecimal;

public class EstoqueInsuficienteException extends RuntimeException {
    public EstoqueInsuficienteException(String nome, BigDecimal disponivel, BigDecimal solicitado) {
        super(String.format(
            "Estoque insuficiente para '%s': disponível %.3f, solicitado %.3f",
            nome, disponivel, solicitado));
    }
}
