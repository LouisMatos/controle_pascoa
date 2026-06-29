package br.com.seuprojeto.pascoa.product.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProdutoAtributoJpaRepository extends JpaRepository<ProdutoAtributoJpaEntity, Long> {
    List<ProdutoAtributoJpaEntity> findByProdutoId(Long produtoId);
    Optional<ProdutoAtributoJpaEntity> findByProdutoIdAndAtributoKey(Long produtoId, String chave);
    void deleteByProdutoIdAndAtributoKey(Long produtoId, String chave);
}
