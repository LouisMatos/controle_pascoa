package br.com.seuprojeto.pascoa.product.domain.exception;

public class ProdutoNotFoundException extends RuntimeException {
    public ProdutoNotFoundException(Long id) {
        super("Produto não encontrado: " + id);
    }
}
