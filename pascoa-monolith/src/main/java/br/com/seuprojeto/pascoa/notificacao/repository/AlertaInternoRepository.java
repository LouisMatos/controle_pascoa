package br.com.seuprojeto.pascoa.notificacao.repository;

import br.com.seuprojeto.pascoa.notificacao.entity.AlertaInterno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertaInternoRepository extends JpaRepository<AlertaInterno, Long> {

    long countByLidoFalse();

    /** Últimos 20 alertas, não lidos primeiro. */
    List<AlertaInterno> findTop20ByOrderByLidoAscCriadoEmDesc();

    /** Lista completa ordenada por mais recente. */
    List<AlertaInterno> findAllByOrderByCriadoEmDesc();

    /** Apenas não-lidos, para filtro. */
    List<AlertaInterno> findByLidoFalseOrderByCriadoEmDesc();

    @Modifying
    @Query("UPDATE AlertaInterno a SET a.lido = true WHERE a.lido = false")
    void marcarTodasLidas();

    @Modifying
    @Query("DELETE FROM AlertaInterno a WHERE a.lido = true")
    int excluirLidas();
}
