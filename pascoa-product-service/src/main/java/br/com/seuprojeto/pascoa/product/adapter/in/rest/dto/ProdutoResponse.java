package br.com.seuprojeto.pascoa.product.adapter.in.rest.dto;

import br.com.seuprojeto.pascoa.product.domain.model.Categoria;
import br.com.seuprojeto.pascoa.product.domain.model.Produto;

import java.math.BigDecimal;

public record ProdutoResponse(
        Long id,
        String nome,
        String descricao,
        BigDecimal preco,
        Categoria categoria,
        String fotoUrl,
        boolean disponivel,
        boolean ativo,
        boolean temFoto
) {
    public static ProdutoResponse from(Produto p) {
        return new ProdutoResponse(
                p.getId(), p.getNome(), p.getDescricao(), p.getPreco(),
                p.getCategoria(), p.getFotoUrl(), p.isDisponivel(),
                p.isAtivo(), p.temFoto()
        );
    }
}
