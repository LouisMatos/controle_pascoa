package br.com.seuprojeto.pascoa.inventory.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MateriaPrimaJpaRepository extends JpaRepository<MateriaPrimaJpaEntity, Long> {
    List<MateriaPrimaJpaEntity> findAllByAtivoTrue();

    @Query("SELECT m FROM MateriaPrimaJpaEntity m WHERE m.ativo = true AND m.quantidadeEstoque <= m.estoqueMinimo")
    List<MateriaPrimaJpaEntity> findCriticos();
}
