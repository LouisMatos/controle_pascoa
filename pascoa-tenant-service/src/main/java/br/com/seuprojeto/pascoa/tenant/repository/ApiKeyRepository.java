package br.com.seuprojeto.pascoa.tenant.repository;

import br.com.seuprojeto.pascoa.tenant.domain.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    Optional<ApiKey> findByChaveHashAndAtivaTrue(String chaveHash);
    List<ApiKey> findByTenantIdOrderByCriadaEmDesc(String tenantId);
}
