package br.com.seuprojeto.pascoa.product.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProdutoVarianteJpaRepository extends JpaRepository<ProdutoVarianteJpaEntity, Long> {
    List<ProdutoVarianteJpaEntity> findByProdutoId(Long produtoId);
    Optional<ProdutoVarianteJpaEntity> findBySku(String sku);
}
