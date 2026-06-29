package br.com.seuprojeto.pascoa.tenant.repository;

import br.com.seuprojeto.pascoa.tenant.domain.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, String> {
    Optional<Tenant> findByDominioCustom(String dominioCustom);
    boolean existsByEmail(String email);
}
