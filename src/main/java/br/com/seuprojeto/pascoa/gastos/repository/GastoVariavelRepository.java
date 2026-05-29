package br.com.seuprojeto.pascoa.gastos.repository;

import br.com.seuprojeto.pascoa.gastos.entity.CategoriaGasto;
import br.com.seuprojeto.pascoa.gastos.entity.GastoVariavel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface GastoVariavelRepository extends JpaRepository<GastoVariavel, Long> {

    List<GastoVariavel> findByReferenciaAnoAndReferenciaMesOrderByDataLancamentoDesc(
            int ano, int mes);

    List<GastoVariavel> findByReferenciaAnoAndReferenciaMesAndCategoriaOrderByDataLancamentoDesc(
            int ano, int mes, CategoriaGasto categoria);

    @Query("SELECT g.categoria, SUM(g.valor) FROM GastoVariavel g " +
           "WHERE g.referenciaAno = :ano AND g.referenciaMes = :mes " +
           "AND g.desconsiderarNoCusto = false " +
           "GROUP BY g.categoria ORDER BY g.categoria")
    List<Object[]> sumPorCategoria(@Param("ano") int ano, @Param("mes") int mes);

    @Query("SELECT COALESCE(SUM(g.valor), 0) FROM GastoVariavel g " +
           "WHERE g.referenciaAno = :ano AND g.referenciaMes = :mes " +
           "AND g.desconsiderarNoCusto = false")
    BigDecimal sumTotal(@Param("ano") int ano, @Param("mes") int mes);

    /** Soma de gastos cujo data_lancamento cai dentro do período (para FluxoCaixa). */
    @Query("SELECT COALESCE(SUM(g.valor), 0) FROM GastoVariavel g " +
           "WHERE g.dataLancamento >= :inicio AND g.dataLancamento <= :fim " +
           "AND g.desconsiderarNoCusto = false")
    BigDecimal sumTotalByPeriodo(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    /** F6: Marca todos os gastos vinculados a um pedido como desconsiderados no custo (cancelamento). */
    @Modifying
    @Query("UPDATE GastoVariavel g SET g.desconsiderarNoCusto = true WHERE g.pedidoId = :pedidoId")
    void desconsiderarPorPedido(@Param("pedidoId") Long pedidoId);
}
