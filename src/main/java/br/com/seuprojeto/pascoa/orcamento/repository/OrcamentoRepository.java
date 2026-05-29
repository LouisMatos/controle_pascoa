package br.com.seuprojeto.pascoa.orcamento.repository;

import br.com.seuprojeto.pascoa.orcamento.entity.Orcamento;
import br.com.seuprojeto.pascoa.orcamento.entity.StatusOrcamento;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OrcamentoRepository extends JpaRepository<Orcamento, Long> {

    @Query("SELECT o FROM Orcamento o JOIN FETCH o.cliente ORDER BY o.dataCriacao DESC")
    List<Orcamento> findAllComCliente();

    @Query("SELECT o FROM Orcamento o JOIN FETCH o.cliente JOIN FETCH o.itens i JOIN FETCH i.produto WHERE o.id = :id")
    Optional<Orcamento> findByIdComItens(Long id);

    Optional<Orcamento> findByTokenAprovacao(String token);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Orcamento o WHERE o.id = :id")
    Optional<Orcamento> findByIdForUpdate(Long id);

    /**
     * Item 25: Orçamentos PENDENTES cuja validade é exatamente a data informada.
     * Usado pelo job de notificação de orçamento expirando (chama com hoje + 2 dias).
     */
    @Query("SELECT o FROM Orcamento o JOIN FETCH o.cliente WHERE o.status = :status AND o.validade = :data")
    List<Orcamento> findPendentesComValidadeEm(@Param("status") StatusOrcamento status,
                                               @Param("data") LocalDate data);
}
