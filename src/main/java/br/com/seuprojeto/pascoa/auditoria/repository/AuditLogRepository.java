package br.com.seuprojeto.pascoa.auditoria.repository;

import br.com.seuprojeto.pascoa.auditoria.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findAllByOrderByCriadoEmDesc(Pageable pageable);

    @Query("""
        SELECT a FROM AuditLog a
        WHERE (:usuario IS NULL OR LOWER(a.usuario) LIKE LOWER(CONCAT('%', :usuario, '%')))
          AND (:acao    IS NULL OR a.acao = :acao)
        ORDER BY a.criadoEm DESC
        """)
    Page<AuditLog> filtrar(@Param("usuario") String usuario,
                           @Param("acao")    String acao,
                           Pageable pageable);
}
