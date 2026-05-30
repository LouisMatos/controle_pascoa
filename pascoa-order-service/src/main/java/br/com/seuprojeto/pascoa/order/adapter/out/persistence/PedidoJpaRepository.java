package br.com.seuprojeto.pascoa.order.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PedidoJpaRepository extends JpaRepository<PedidoJpaEntity, Long> {
    Optional<PedidoJpaEntity> findByTokenRastreamento(String token);
}
