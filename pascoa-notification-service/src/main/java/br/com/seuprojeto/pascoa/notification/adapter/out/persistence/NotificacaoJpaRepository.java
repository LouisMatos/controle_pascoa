package br.com.seuprojeto.pascoa.notification.adapter.out.persistence;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacaoJpaRepository extends JpaRepository<NotificacaoJpaEntity, Long> {
    List<NotificacaoJpaEntity> findByReferenciaIdOrderByCriadoEmDesc(String referenciaId);
    List<NotificacaoJpaEntity> findAllByOrderByCriadoEmDesc(PageRequest pageRequest);
}
