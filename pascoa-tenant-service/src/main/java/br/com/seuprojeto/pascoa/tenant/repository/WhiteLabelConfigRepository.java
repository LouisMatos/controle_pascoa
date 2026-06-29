package br.com.seuprojeto.pascoa.tenant.repository;

import br.com.seuprojeto.pascoa.tenant.domain.WhiteLabelConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WhiteLabelConfigRepository extends JpaRepository<WhiteLabelConfig, String> {
}
