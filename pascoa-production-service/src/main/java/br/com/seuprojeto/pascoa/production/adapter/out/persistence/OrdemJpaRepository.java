package br.com.seuprojeto.pascoa.production.adapter.out.persistence;

import br.com.seuprojeto.pascoa.production.domain.model.StatusOrdem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrdemJpaRepository extends JpaRepository<OrdemProducaoJpaEntity, Long> {
    Optional<OrdemProducaoJpaEntity> findByPedidoId(Long pedidoId);
    List<OrdemProducaoJpaEntity> findByStatus(StatusOrdem status);
}
