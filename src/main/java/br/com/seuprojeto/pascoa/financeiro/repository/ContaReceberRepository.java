package br.com.seuprojeto.pascoa.financeiro.repository;

import br.com.seuprojeto.pascoa.financeiro.entity.ContaReceber;
import br.com.seuprojeto.pascoa.financeiro.entity.StatusConta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ContaReceberRepository extends JpaRepository<ContaReceber, Long> {

    List<ContaReceber> findByStatusOrderByVencimento(StatusConta status);

    List<ContaReceber> findAllByOrderByVencimento();

    @Query("""
        SELECT COALESCE(SUM(c.valorOriginal - COALESCE(c.valorPago, 0)), 0)
        FROM ContaReceber c
        WHERE c.status = 'ABERTA' AND c.vencimento BETWEEN :inicio AND :fim
        """)
    BigDecimal sumPrevistoEntrada(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    List<ContaReceber> findByPedidoId(Long pedidoId);
}
