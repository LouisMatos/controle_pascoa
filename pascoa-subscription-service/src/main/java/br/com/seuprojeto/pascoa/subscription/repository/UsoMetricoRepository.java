package br.com.seuprojeto.pascoa.subscription.repository;

import br.com.seuprojeto.pascoa.subscription.domain.UsoMetrico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsoMetricoRepository extends JpaRepository<UsoMetrico, UsoMetrico.PK> {
    Optional<UsoMetrico> findByTenantIdAndCompetencia(String tenantId, String competencia);
    List<UsoMetrico> findByTenantIdOrderByCompetenciaDesc(String tenantId);
}
