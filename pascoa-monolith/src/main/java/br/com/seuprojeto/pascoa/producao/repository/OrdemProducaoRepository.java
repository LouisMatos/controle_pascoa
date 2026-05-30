package br.com.seuprojeto.pascoa.producao.repository;

import br.com.seuprojeto.pascoa.producao.entity.OrdemProducao;
import br.com.seuprojeto.pascoa.producao.entity.StatusOrdem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrdemProducaoRepository extends JpaRepository<OrdemProducao, Long> {

    @Query("SELECT o FROM OrdemProducao o LEFT JOIN FETCH o.pedido LEFT JOIN FETCH o.produto ORDER BY o.dataAbertura DESC")
    List<OrdemProducao> findAllComDetalhes();

    @Query("SELECT o FROM OrdemProducao o LEFT JOIN FETCH o.pedido LEFT JOIN FETCH o.produto WHERE o.status = :status ORDER BY o.dataAbertura ASC")
    List<OrdemProducao> findByStatusComDetalhes(@Param("status") StatusOrdem status);

    @Query("SELECT o FROM OrdemProducao o LEFT JOIN FETCH o.pedido LEFT JOIN FETCH o.produto WHERE o.pedido.id = :pedidoId ORDER BY o.dataAbertura ASC")
    List<OrdemProducao> findByPedidoId(@Param("pedidoId") Long pedidoId);

    @Query("SELECT o FROM OrdemProducao o LEFT JOIN FETCH o.pedido LEFT JOIN FETCH o.produto WHERE o.id = :id")
    Optional<OrdemProducao> findByIdComDetalhes(@Param("id") Long id);

    long countByStatus(StatusOrdem status);
}
