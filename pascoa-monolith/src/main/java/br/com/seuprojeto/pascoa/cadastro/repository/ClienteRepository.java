package br.com.seuprojeto.pascoa.cadastro.repository;

import br.com.seuprojeto.pascoa.common.tenant.TenantContext;
import br.com.seuprojeto.pascoa.cadastro.dto.ClienteComboDto;
import br.com.seuprojeto.pascoa.cadastro.entity.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    @Query("SELECT c FROM Cliente c WHERE c.excluidoEm IS NULL ORDER BY c.nome ASC")
    List<Cliente> findAllByOrderByNomeAsc();

    /** Projeção leve para combos/selects (id + nome), já ordenada — evita carregar a entidade inteira. */
    @Query("SELECT new br.com.seuprojeto.pascoa.cadastro.dto.ClienteComboDto(c.id, c.nome) " +
           "FROM Cliente c WHERE c.excluidoEm IS NULL ORDER BY c.nome ASC")
    List<ClienteComboDto> findAllComboBox();

    @Query("SELECT c FROM Cliente c WHERE c.excluidoEm IS NULL "
         + "AND LOWER(c.nome) LIKE LOWER(CONCAT('%', :nome, '%')) ORDER BY c.nome ASC")
    List<Cliente> findByNomeContainingIgnoreCaseOrderByNomeAsc(@Param("nome") String nome);

    @Query("SELECT c FROM Cliente c WHERE c.excluidoEm IS NULL ORDER BY c.nome ASC")
    Page<Cliente> findPaginado(Pageable pageable);

    @Query("SELECT c FROM Cliente c WHERE c.excluidoEm IS NULL "
         + "AND LOWER(c.nome) LIKE LOWER(CONCAT('%', :nome, '%')) ORDER BY c.nome ASC")
    Page<Cliente> buscarPorNomePaginado(@Param("nome") String nome, Pageable pageable);

    /**
     * Item 25: Clientes cujo aniversário é hoje (por mês e dia), com opt-in ativo.
     * SQL nativo com EXTRACT para compatibilidade PostgreSQL e H2.
     */
    @Query(value = "SELECT * FROM clientes " +
                   "WHERE data_nascimento IS NOT NULL " +
                   "AND EXTRACT(MONTH FROM data_nascimento) = :mes " +
                   "AND EXTRACT(DAY FROM data_nascimento) = :dia " +
                   "AND opt_in = TRUE " +
                   "AND excluido_em IS NULL " +
                   "AND loja_id = " + TenantContext.LOJA_ATUAL_SPEL,
           nativeQuery = true)
    List<Cliente> findAniversariantesHoje(@Param("mes") int mes, @Param("dia") int dia);

    @Query("SELECT c FROM Cliente c WHERE c.excluidoEm IS NULL AND c.id = :id")
    java.util.Optional<Cliente> findVigenteById(@Param("id") Long id);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
}
