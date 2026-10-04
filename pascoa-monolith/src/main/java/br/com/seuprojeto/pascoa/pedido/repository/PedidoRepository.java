package br.com.seuprojeto.pascoa.pedido.repository;

import br.com.seuprojeto.pascoa.pedido.entity.Pedido;
import br.com.seuprojeto.pascoa.pedido.entity.StatusPedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    @Query("SELECT p FROM Pedido p LEFT JOIN FETCH p.cliente ORDER BY p.dataPedido DESC")
    List<Pedido> findAllComCliente();

    @Query(value = "SELECT p FROM Pedido p LEFT JOIN FETCH p.cliente ORDER BY p.dataPedido DESC",
           countQuery = "SELECT count(p) FROM Pedido p")
    Page<Pedido> findComCliente(Pageable pageable);

    @Query(value = "SELECT p FROM Pedido p LEFT JOIN FETCH p.cliente WHERE p.status = :status ORDER BY p.dataPedido DESC",
           countQuery = "SELECT count(p) FROM Pedido p WHERE p.status = :status")
    Page<Pedido> findByStatusComCliente(@Param("status") StatusPedido status, Pageable pageable);

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

    @Query("SELECT COALESCE(SUM(p.totalPedido), 0) FROM Pedido p " +
           "WHERE p.status IN :statuses " +
           "AND MONTH(p.dataPedido) = :mes AND YEAR(p.dataPedido) = :ano")
    BigDecimal sumTotalPorStatusAndMes(@Param("statuses") List<StatusPedido> statuses,
                                       @Param("mes") int mes, @Param("ano") int ano);

    @Query("""
        SELECT p.id, c.nome, p.dataEntrega, p.dataPedido,
               p.totalPedido - COALESCE((SELECT SUM(g.valor) FROM Pagamento g WHERE g.pedido = p), 0)
        FROM Pedido p LEFT JOIN p.cliente c
        WHERE p.status <> br.com.seuprojeto.pascoa.pedido.entity.StatusPedido.CANCELADO
          AND p.totalPedido > COALESCE((SELECT SUM(g.valor) FROM Pagamento g WHERE g.pedido = p), 0)
        ORDER BY p.dataEntrega
    """)
    List<Object[]> saldosEmAberto();

    @Query("""
        SELECT COALESCE(SUM(p.totalPedido - COALESCE((SELECT SUM(g.valor) FROM Pagamento g WHERE g.pedido = p), 0)), 0)
        FROM Pedido p
        WHERE p.status <> br.com.seuprojeto.pascoa.pedido.entity.StatusPedido.CANCELADO
          AND p.dataEntrega BETWEEN :inicio AND :fim
          AND p.totalPedido > COALESCE((SELECT SUM(g.valor) FROM Pagamento g WHERE g.pedido = p), 0)
    """)
    BigDecimal sumSaldoEmAbertoPorVencimento(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    @Query("""
        SELECT p FROM Pedido p LEFT JOIN FETCH p.cliente
        WHERE p.dataEntrega = :data AND p.status IN :statuses
        ORDER BY p.slotEntrega, p.id
    """)
    List<Pedido> findPorDataEntrega(@Param("data") LocalDate data,
                                    @Param("statuses") List<StatusPedido> statuses,
                                    Pageable pageable);

    long countByDataEntregaAndStatusIn(LocalDate dataEntrega, List<StatusPedido> statuses);

    @Query("""
        SELECT p FROM Pedido p LEFT JOIN FETCH p.cliente
        WHERE p.dataEntrega < :data AND p.status IN :statuses
        ORDER BY p.dataEntrega, p.id
    """)
    List<Pedido> findAtrasados(@Param("data") LocalDate data,
                               @Param("statuses") List<StatusPedido> statuses,
                               Pageable pageable);

    long countByDataEntregaBeforeAndStatusIn(LocalDate dataEntrega, List<StatusPedido> statuses);
}
