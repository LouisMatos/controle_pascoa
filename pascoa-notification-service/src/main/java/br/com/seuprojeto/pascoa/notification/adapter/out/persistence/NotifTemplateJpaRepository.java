package br.com.seuprojeto.pascoa.notification.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NotifTemplateJpaRepository extends JpaRepository<NotifTemplateJpaEntity, Long> {

    /** Resolve com fallback: 1º tenta tenant; 2º (se não houver) global. */
    @Query("""
           SELECT t FROM NotifTemplateJpaEntity t
            WHERE t.evento = :evento AND t.canal = :canal AND t.ativo = true
              AND (t.tenantId = :tenantId OR t.tenantId IS NULL)
            ORDER BY CASE WHEN t.tenantId IS NULL THEN 1 ELSE 0 END
           """)
    List<NotifTemplateJpaEntity> resolverComFallback(
            @Param("tenantId") String tenantId,
            @Param("evento")   String evento,
            @Param("canal")    String canal);

    Optional<NotifTemplateJpaEntity> findByTenantIdAndEventoAndCanalAndAtivoTrue(
            String tenantId, String evento, String canal);

    List<NotifTemplateJpaEntity> findByTenantIdAndAtivoTrue(String tenantId);
}
