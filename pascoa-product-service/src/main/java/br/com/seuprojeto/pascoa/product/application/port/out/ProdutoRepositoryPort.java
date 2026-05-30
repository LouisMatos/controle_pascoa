package br.com.seuprojeto.pascoa.product.application.port.out;

import br.com.seuprojeto.pascoa.product.domain.model.Categoria;
import br.com.seuprojeto.pascoa.product.domain.model.Produto;

import java.util.List;
import java.util.Optional;

public interface ProdutoRepositoryPort {
    Optional<Produto> findById(Long id);
    List<Produto> findAllAtivos();
    List<Produto> findDisponiveis();
    List<Produto> findByCategoria(Categoria categoria);
    Produto save(Produto produto);
}
