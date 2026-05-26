package br.com.seuprojeto.pascoa.pedido.repository;

import br.com.seuprojeto.pascoa.pedido.entity.Pedido;
import br.com.seuprojeto.pascoa.pedido.entity.StatusPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    @Query("SELECT p FROM Pedido p LEFT JOIN FETCH p.cliente ORDER BY p.dataPedido DESC")
    List<Pedido> findAllComCliente();

    @Query("SELECT p FROM Pedido p LEFT JOIN FETCH p.cliente WHERE p.status = :status ORDER BY p.dataPedido DESC")
    List<Pedido> findByStatusComCliente(@Param("status") StatusPedido status);

    @Query("SELECT DISTINCT p FROM Pedido p " +
           "LEFT JOIN FETCH p.itens i " +
           "LEFT JOIN FETCH i.produto " +
           "WHERE p.id = :id")
    Optional<Pedido> findByIdComItens(@Param("id") Long id);

    long countByStatusIn(List<StatusPedido> statuses);

    long countByStatus(StatusPedido status);

    @Query("SELECT COALESCE(SUM(p.totalPedido), 0) FROM Pedido p WHERE p.status = :status")
    BigDecimal sumTotalPorStatus(@Param("status") StatusPedido status);

    @Query("SELECT COALESCE(SUM(p.totalPedido), 0) FROM Pedido p WHERE p.status IN :statuses")
    BigDecimal sumTotalPorStatuses(@Param("statuses") List<StatusPedido> statuses);

    @Query("SELECT DISTINCT p FROM Pedido p " +
           "LEFT JOIN FETCH p.cliente " +
           "LEFT JOIN FETCH p.itens i " +
           "LEFT JOIN FETCH i.produto " +
           "WHERE p.tokenAcompanhamento = :token")
    Optional<Pedido> findByTokenAcompanhamento(@Param("token") String token);

    // ── CRM ───────────────────────────────────────────────────────────────

    /** Retorna [clienteId, ltv, totalPedidos, ultimoPedido] agrupado por cliente (exclui CANCELADO). */
    @Query("SELECT p.cliente.id, COALESCE(SUM(p.totalPedido), 0), COUNT(p), MAX(p.dataPedido) " +
           "FROM Pedido p WHERE p.status <> br.com.seuprojeto.pascoa.pedido.entity.StatusPedido.CANCELADO " +
           "GROUP BY p.cliente.id")
    List<Object[]> statsPorCliente();

    /** Últimos 5 pedidos de um cliente. */
    @Query("SELECT p FROM Pedido p LEFT JOIN FETCH p.itens WHERE p.cliente.id = :clienteId " +
           "ORDER BY p.dataPedido DESC LIMIT 5")
    List<Pedido> ultimosPedidosPorCliente(@Param("clienteId") Long clienteId);

    // ── Analytics ─────────────────────────────────────────────────────────

    /** Faturamento e contagem por mês de um ano: [mes(int), faturamento, count] */
    @Query(value = "SELECT EXTRACT(MONTH FROM data_pedido)::int, COALESCE(SUM(total_pedido),0), COUNT(*) " +
                   "FROM pedidos WHERE EXTRACT(YEAR FROM data_pedido) = :ano AND status != 'CANCELADO' " +
                   "GROUP BY 1 ORDER BY 1", nativeQuery = true)
    List<Object[]> faturamentoPorMes(@Param("ano") int ano);

    /** Total de faturamento de um ano (excluindo CANCELADO). */
    @Query(value = "SELECT COALESCE(SUM(total_pedido), 0) FROM pedidos " +
                   "WHERE EXTRACT(YEAR FROM data_pedido) = :ano AND status != 'CANCELADO'",
           nativeQuery = true)
    BigDecimal totalPorAno(@Param("ano") int ano);

    /** Total de pedidos de um ano (excluindo CANCELADO). */
    @Query(value = "SELECT COUNT(*) FROM pedidos " +
                   "WHERE EXTRACT(YEAR FROM data_pedido) = :ano AND status != 'CANCELADO'",
           nativeQuery = true)
    long countPorAno(@Param("ano") int ano);

    /** Anos distintos que têm pedidos (ordenado DESC). */
    @Query(value = "SELECT DISTINCT EXTRACT(YEAR FROM data_pedido)::int FROM pedidos " +
                   "WHERE status != 'CANCELADO' ORDER BY 1 DESC", nativeQuery = true)
    List<Integer> anosComPedidos();
}
