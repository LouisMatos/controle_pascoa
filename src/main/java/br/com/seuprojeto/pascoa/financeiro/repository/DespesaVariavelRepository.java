package br.com.seuprojeto.pascoa.financeiro.repository;

import br.com.seuprojeto.pascoa.financeiro.entity.DespesaVariavel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface DespesaVariavelRepository extends JpaRepository<DespesaVariavel, Long> {

    List<DespesaVariavel> findByPedidoIdOrderByCategoria(Long pedidoId);

    @Query("SELECT COALESCE(SUM(d.valor), 0) FROM DespesaVariavel d WHERE d.pedido.id = :pedidoId")
    BigDecimal sumPorPedido(@Param("pedidoId") Long pedidoId);
}
