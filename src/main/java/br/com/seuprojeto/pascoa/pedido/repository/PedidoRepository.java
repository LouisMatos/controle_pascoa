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
}
