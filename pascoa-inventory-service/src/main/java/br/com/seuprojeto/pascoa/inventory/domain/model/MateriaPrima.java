package br.com.seuprojeto.pascoa.inventory.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.With;

import java.math.BigDecimal;

@Getter
@Builder
@With
public class MateriaPrima {

    private final Long id;
    private final String nome;
    private final Unidade unidade;
    private final BigDecimal quantidadeEstoque;
    private final BigDecimal estoqueMinimo;
    private final Long fornecedorId;
    private final boolean ativo;

    public boolean estoqueEstaCritico() {
        return quantidadeEstoque.compareTo(estoqueMinimo) <= 0;
    }

    public MateriaPrima registrarEntrada(BigDecimal quantidade) {
        return this.withQuantidadeEstoque(quantidadeEstoque.add(quantidade));
    }

    public MateriaPrima registrarSaida(BigDecimal quantidade) {
        if (quantidadeEstoque.compareTo(quantidade) < 0) {
            throw new br.com.seuprojeto.pascoa.inventory.domain.exception.EstoqueInsuficienteException(
                    nome, quantidadeEstoque, quantidade);
        }
        return this.withQuantidadeEstoque(quantidadeEstoque.subtract(quantidade));
    }
}
