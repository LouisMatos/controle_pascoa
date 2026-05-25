package br.com.seuprojeto.pascoa.financeiro.repository;

import br.com.seuprojeto.pascoa.financeiro.entity.ContaPagar;
import br.com.seuprojeto.pascoa.financeiro.entity.StatusConta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ContaPagarRepository extends JpaRepository<ContaPagar, Long> {

    List<ContaPagar> findAllByOrderByVencimento();

    List<ContaPagar> findByStatusOrderByVencimento(StatusConta status);

    @Query("""
        SELECT COALESCE(SUM(c.valor), 0)
        FROM ContaPagar c
        WHERE c.status = 'ABERTA' AND c.vencimento BETWEEN :inicio AND :fim
        """)
    BigDecimal sumPrevistoSaida(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);
}
