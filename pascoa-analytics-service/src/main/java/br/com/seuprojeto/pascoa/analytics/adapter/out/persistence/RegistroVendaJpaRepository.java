package br.com.seuprojeto.pascoa.analytics.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegistroVendaJpaRepository extends JpaRepository<RegistroVendaJpaEntity, Long> {
    List<RegistroVendaJpaEntity> findByAno(int ano);
    boolean existsByPedidoId(Long pedidoId);
}
