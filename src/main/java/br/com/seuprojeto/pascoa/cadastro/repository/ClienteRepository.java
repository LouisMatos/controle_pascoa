package br.com.seuprojeto.pascoa.cadastro.repository;

import br.com.seuprojeto.pascoa.cadastro.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    List<Cliente> findAllByOrderByNomeAsc();

    List<Cliente> findByNomeContainingIgnoreCaseOrderByNomeAsc(String nome);

    /**
     * Item 25: Clientes cujo aniversário é hoje (por mês e dia), com opt-in ativo.
     * SQL nativo com EXTRACT para compatibilidade PostgreSQL e H2.
     */
    @Query(value = "SELECT * FROM clientes " +
                   "WHERE data_nascimento IS NOT NULL " +
                   "AND EXTRACT(MONTH FROM data_nascimento) = :mes " +
                   "AND EXTRACT(DAY FROM data_nascimento) = :dia " +
                   "AND opt_in = TRUE " +
                   "AND excluido_em IS NULL",
           nativeQuery = true)
    List<Cliente> findAniversariantesHoje(@Param("mes") int mes, @Param("dia") int dia);
}
