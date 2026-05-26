package br.com.seuprojeto.pascoa.qualidade.repository;

import br.com.seuprojeto.pascoa.qualidade.entity.ChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, Long> {

    List<ChecklistItem> findByProdutoIdOrderByOrdemAscIdAsc(Long produtoId);

    List<ChecklistItem> findByProdutoIdAndAtivoTrueOrderByOrdemAscIdAsc(Long produtoId);

    long countByProdutoId(Long produtoId);
}
