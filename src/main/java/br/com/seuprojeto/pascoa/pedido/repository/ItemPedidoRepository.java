package br.com.seuprojeto.pascoa.pedido.repository;

import br.com.seuprojeto.pascoa.pedido.entity.ItemPedido;
import br.com.seuprojeto.pascoa.pedido.entity.StatusPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemPedidoRepository extends JpaRepository<ItemPedido, Long> {

    boolean existsByPedidoIdAndProdutoId(Long pedidoId, Long produtoId);

    @Query("SELECT i.produto.nome, SUM(i.quantidade), SUM(i.subtotal) " +
           "FROM ItemPedido i " +
           "WHERE i.pedido.status != :cancelado " +
           "GROUP BY i.produto.id, i.produto.nome " +
           "ORDER BY SUM(i.quantidade) DESC")
    List<Object[]> topProdutos(@Param("cancelado") StatusPedido cancelado);

    /** Ranking de produtos por ano: [nome, categoria, qtd, faturamento] */
    @Query(value = "SELECT pr.nome, pr.categoria, SUM(i.quantidade)::bigint, COALESCE(SUM(i.subtotal), 0) " +
                   "FROM itens_pedido i " +
                   "JOIN produtos pr ON i.produto_id = pr.id " +
                   "JOIN pedidos ped ON i.pedido_id = ped.id " +
                   "WHERE EXTRACT(YEAR FROM ped.data_pedido) = :ano AND ped.status != 'CANCELADO' " +
                   "GROUP BY pr.id, pr.nome, pr.categoria " +
                   "ORDER BY SUM(i.quantidade) DESC LIMIT 15",
           nativeQuery = true)
    List<Object[]> rankingProdutosPorAno(@Param("ano") int ano);
}
