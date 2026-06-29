package br.com.seuprojeto.pascoa.tenant.repository;

import br.com.seuprojeto.pascoa.tenant.domain.FeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, String> {
}
