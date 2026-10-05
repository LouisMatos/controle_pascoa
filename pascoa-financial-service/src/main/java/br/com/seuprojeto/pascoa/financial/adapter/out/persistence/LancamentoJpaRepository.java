package br.com.seuprojeto.pascoa.financial.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LancamentoJpaRepository extends JpaRepository<LancamentoJpaEntity, Long> {

    @Query("SELECT l FROM LancamentoJpaEntity l WHERE MONTH(l.data) = :mes AND YEAR(l.data) = :ano ORDER BY l.data")
    List<LancamentoJpaEntity> findByMesAno(@Param("mes") int mes, @Param("ano") int ano);

    List<LancamentoJpaEntity> findByDataBetweenOrderByData(LocalDate inicio, LocalDate fim);

    @Query("SELECT l FROM LancamentoJpaEntity l WHERE YEAR(l.data) = :ano ORDER BY l.data")
    List<LancamentoJpaEntity> findByAno(@Param("ano") int ano);

    boolean existsByReferenciaIdAndOrigem(String referenciaId, String origem);
}
