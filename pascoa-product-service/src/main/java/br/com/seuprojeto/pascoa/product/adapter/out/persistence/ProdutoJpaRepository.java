package br.com.seuprojeto.pascoa.product.adapter.out.persistence;

import br.com.seuprojeto.pascoa.product.domain.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProdutoJpaRepository extends JpaRepository<ProdutoJpaEntity, Long> {
    List<ProdutoJpaEntity> findAllByAtivoTrue();
    List<ProdutoJpaEntity> findAllByAtivoTrueAndDisponivelTrue();
    List<ProdutoJpaEntity> findAllByAtivoTrueAndCategoria(Categoria categoria);
}
