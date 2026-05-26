package br.com.seuprojeto.pascoa.qualidade.repository;

import br.com.seuprojeto.pascoa.qualidade.entity.InspecaoQualidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InspecaoRepository extends JpaRepository<InspecaoQualidade, Long> {

    Optional<InspecaoQualidade> findByOrdemProducaoId(Long ordemId);

    @Query("SELECT i FROM InspecaoQualidade i JOIN FETCH i.ordemProducao o JOIN FETCH o.produto ORDER BY i.dataInspecao DESC")
    List<InspecaoQualidade> findAllComDetalhes();

    long countByAprovadoTrue();
    long countByAprovadoFalse();
}
