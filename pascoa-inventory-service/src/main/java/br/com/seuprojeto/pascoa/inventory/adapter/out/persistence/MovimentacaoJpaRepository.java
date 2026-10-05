package br.com.seuprojeto.pascoa.inventory.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimentacaoJpaRepository extends JpaRepository<MovimentacaoJpaEntity, Long> {
    List<MovimentacaoJpaEntity> findByMateriaPrimaIdOrderByDataMovimentacaoDesc(Long materiaPrimaId);
}
