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

    /**
     * F9: Calcula saldo considerando apenas créditos não expirados.
     * - DEBITO: sempre deduzido (sem expiração)
     * - CREDITO: só conta se data_expiracao IS NULL ou ainda no futuro
     */
    @Query(value = """
            SELECT COALESCE(SUM(
                CASE
                    WHEN tipo = 'CREDITO'
                         AND (data_expiracao IS NULL OR data_expiracao >= CURRENT_DATE)
                    THEN pontos
                    WHEN tipo = 'DEBITO'
                    THEN -pontos
                    ELSE 0
                END
            ), 0)
            FROM pontos_fidelidade
            WHERE cliente_id = :clienteId
            """, nativeQuery = true)
    int saldoPorCliente(@Param("clienteId") Long clienteId);
}
