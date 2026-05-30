package br.com.seuprojeto.pascoa.product.application.port.out;

import br.com.seuprojeto.pascoa.product.domain.model.Produto;

public interface ProdutoEventPublisherPort {
    void publishProdutoCriado(Produto produto);
    void publishProdutoAtualizado(Produto produto);
    void publishProdutoInativado(Long produtoId);
}
