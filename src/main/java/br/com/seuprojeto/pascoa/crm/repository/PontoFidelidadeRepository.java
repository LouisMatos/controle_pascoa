package br.com.seuprojeto.pascoa.crm.repository;

import br.com.seuprojeto.pascoa.crm.entity.PontoFidelidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PontoFidelidadeRepository extends JpaRepository<PontoFidelidade, Long> {

    List<PontoFidelidade> findByClienteIdOrderByDataOperacaoDesc(Long clienteId);

    @Query(value = """
            SELECT COALESCE(SUM(CASE WHEN tipo = 'CREDITO' THEN pontos ELSE -pontos END), 0)
            FROM pontos_fidelidade
            WHERE cliente_id = :clienteId
            """, nativeQuery = true)
    int saldoPorCliente(@Param("clienteId") Long clienteId);
}
