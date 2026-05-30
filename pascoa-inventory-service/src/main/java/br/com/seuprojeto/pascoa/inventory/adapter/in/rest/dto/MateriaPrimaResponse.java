package br.com.seuprojeto.pascoa.inventory.adapter.in.rest.dto;

import br.com.seuprojeto.pascoa.inventory.domain.model.MateriaPrima;
import br.com.seuprojeto.pascoa.inventory.domain.model.Unidade;

import java.math.BigDecimal;

public record MateriaPrimaResponse(
        Long id,
        String nome,
        Unidade unidade,
        BigDecimal quantidadeEstoque,
        BigDecimal estoqueMinimo,
        Long fornecedorId,
        boolean ativo,
        boolean estoqueCritico
) {
    public static MateriaPrimaResponse from(MateriaPrima mp) {
        return new MateriaPrimaResponse(
                mp.getId(), mp.getNome(), mp.getUnidade(),
                mp.getQuantidadeEstoque(), mp.getEstoqueMinimo(),
                mp.getFornecedorId(), mp.isAtivo(),
                mp.estoqueEstaCritico()
        );
    }
}
