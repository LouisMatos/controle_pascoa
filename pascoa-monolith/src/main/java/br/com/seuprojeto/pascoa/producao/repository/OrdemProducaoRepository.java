package br.com.seuprojeto.pascoa.producao.repository;

import br.com.seuprojeto.pascoa.producao.entity.OrdemProducao;
import br.com.seuprojeto.pascoa.producao.entity.StatusOrdem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrdemProducaoRepository extends JpaRepository<OrdemProducao, Long> {

    @Query(value = "SELECT o FROM OrdemProducao o LEFT JOIN FETCH o.pedido LEFT JOIN FETCH o.produto ORDER BY o.dataAbertura DESC",
           countQuery = "SELECT count(o) FROM OrdemProducao o")
    Page<OrdemProducao> findComDetalhes(Pageable pageable);

    @Query(value = "SELECT o FROM OrdemProducao o LEFT JOIN FETCH o.pedido LEFT JOIN FETCH o.produto WHERE o.status = :status ORDER BY o.dataAbertura ASC",
           countQuery = "SELECT count(o) FROM OrdemProducao o WHERE o.status = :status")
    Page<OrdemProducao> findByStatusComDetalhes(@Param("status") StatusOrdem status, Pageable pageable);

    @Query("SELECT o FROM OrdemProducao o LEFT JOIN FETCH o.pedido "
        + "LEFT JOIN FETCH o.produto WHERE o.pedido.id = :pedidoId "
        + "ORDER BY o.dataAbertura ASC")
    List<OrdemProducao> findByPedidoId(@Param("pedidoId") Long pedidoId);

    @Query("SELECT o FROM OrdemProducao o LEFT JOIN FETCH o.pedido LEFT JOIN FETCH o.produto WHERE o.id = :id")
    Optional<OrdemProducao> findByIdComDetalhes(@Param("id") Long id);

    long countByStatus(StatusOrdem status);

    boolean existsByPedidoIdAndStatusIn(Long pedidoId, List<StatusOrdem> status);

    @Query("""
        SELECT o FROM OrdemProducao o
        LEFT JOIN FETCH o.pedido pedido
        LEFT JOIN FETCH pedido.cliente
        LEFT JOIN FETCH o.produto
        WHERE o.status IN :status
        ORDER BY pedido.dataEntrega, o.id
    """)
    List<OrdemProducao> findAbertasPorPrazo(@Param("status") List<StatusOrdem> status, Pageable pageable);

    long countByStatusIn(List<StatusOrdem> status);
}
