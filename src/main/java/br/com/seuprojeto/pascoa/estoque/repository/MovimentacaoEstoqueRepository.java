package br.com.seuprojeto.pascoa.estoque.repository;

import br.com.seuprojeto.pascoa.estoque.entity.MovimentacaoEstoque;
import br.com.seuprojeto.pascoa.estoque.entity.TipoMovimentacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, Long> {

    List<MovimentacaoEstoque> findAllByOrderByDataDesc();

    List<MovimentacaoEstoque> findByMateriaPrimaIdOrderByDataDesc(Long materiaPrimaId);

    List<MovimentacaoEstoque> findByTipoOrderByDataDesc(TipoMovimentacao tipo);

    @Query("SELECT m FROM MovimentacaoEstoque m " +
           "WHERE (:mpId IS NULL OR m.materiaPrima.id = :mpId) " +
           "AND   (:tipo IS NULL OR m.tipo = :tipo) " +
           "ORDER BY m.data DESC")
    List<MovimentacaoEstoque> filtrar(@Param("mpId") Long mpId,
                                      @Param("tipo") TipoMovimentacao tipo);
}
