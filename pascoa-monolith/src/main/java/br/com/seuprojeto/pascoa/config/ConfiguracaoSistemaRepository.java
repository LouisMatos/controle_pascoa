package br.com.seuprojeto.pascoa.config;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfiguracaoSistemaRepository extends JpaRepository<ConfiguracaoSistema, Long> {
}
