package br.com.seuprojeto.pascoa.configengine.repository;

import br.com.seuprojeto.pascoa.configengine.domain.TenantConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantConfigRepository extends JpaRepository<TenantConfig, String> {
}
