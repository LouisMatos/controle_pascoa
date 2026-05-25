package br.com.seuprojeto.pascoa.financeiro.repository;

import br.com.seuprojeto.pascoa.financeiro.entity.ConfiguracaoFinanceira;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfiguracaoFinanceiraRepository extends JpaRepository<ConfiguracaoFinanceira, Long> {

    default ConfiguracaoFinanceira obter() {
        return findAll().stream().findFirst()
            .orElseGet(() -> {
                var cfg = ConfiguracaoFinanceira.builder().build();
                return save(cfg);
            });
    }
}
