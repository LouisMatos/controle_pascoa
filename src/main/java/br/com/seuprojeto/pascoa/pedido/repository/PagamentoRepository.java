package br.com.seuprojeto.pascoa.pedido.repository;

import br.com.seuprojeto.pascoa.pedido.entity.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

    List<Pagamento> findByPedidoIdOrderByDataPagamentoDesc(Long pedidoId);

    @Query("SELECT COALESCE(SUM(p.valor), 0) FROM Pagamento p WHERE p.pedido.id = :pedidoId")
    BigDecimal somarPorPedido(@Param("pedidoId") Long pedidoId);

    @Query("SELECT COALESCE(SUM(p.valor), 0) FROM Pagamento p")
    BigDecimal sumTotal();
}
