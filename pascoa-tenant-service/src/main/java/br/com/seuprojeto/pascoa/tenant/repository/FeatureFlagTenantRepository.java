package br.com.seuprojeto.pascoa.tenant.repository;

import br.com.seuprojeto.pascoa.tenant.domain.FeatureFlagTenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FeatureFlagTenantRepository extends JpaRepository<FeatureFlagTenant, FeatureFlagTenant.PK> {
    List<FeatureFlagTenant> findByTenantId(String tenantId);
    Optional<FeatureFlagTenant> findByTenantIdAndChave(String tenantId, String chave);
}
