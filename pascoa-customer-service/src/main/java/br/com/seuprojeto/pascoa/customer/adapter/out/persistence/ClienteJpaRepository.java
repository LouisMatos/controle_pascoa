package br.com.seuprojeto.pascoa.customer.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClienteJpaRepository extends JpaRepository<ClienteJpaEntity, Long> {
    Optional<ClienteJpaEntity> findByEmail(String email);
    List<ClienteJpaEntity> findAllByAtivoTrue();
    boolean existsByEmail(String email);
}
