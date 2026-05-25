package br.com.seuprojeto.pascoa.financeiro.repository;

import br.com.seuprojeto.pascoa.financeiro.entity.DespesaFixa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface DespesaFixaRepository extends JpaRepository<DespesaFixa, Long> {

    List<DespesaFixa> findByAtivoTrueOrderByDescricao();

    @Query("""
        SELECT COALESCE(SUM(
            CASE d.periodicidade
                WHEN 'MENSAL' THEN d.valor
                WHEN 'ANUAL'  THEN d.valor / 12
            END
        ), 0)
        FROM DespesaFixa d WHERE d.ativo = true
        """)
    BigDecimal sumMensalAtivas();
}
