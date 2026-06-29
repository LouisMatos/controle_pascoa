package br.com.seuprojeto.pascoa.production.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoricoFaseJpaRepository extends JpaRepository<HistoricoFaseJpaEntity, Long> {
    List<HistoricoFaseJpaEntity> findByOrdemIdOrderByMudadoEmAsc(Long ordemId);
}
