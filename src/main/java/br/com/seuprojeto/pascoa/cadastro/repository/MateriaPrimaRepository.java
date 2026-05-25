package br.com.seuprojeto.pascoa.cadastro.repository;

import br.com.seuprojeto.pascoa.cadastro.entity.MateriaPrima;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MateriaPrimaRepository extends JpaRepository<MateriaPrima, Long> {

    List<MateriaPrima> findAllByOrderByNomeAsc();

    List<MateriaPrima> findByNomeContainingIgnoreCaseOrderByNomeAsc(String nome);

    @Query("SELECT m FROM MateriaPrima m WHERE m.quantidadeAtual < m.quantidadeMinima ORDER BY m.nome")
    List<MateriaPrima> findComEstoqueCritico();
}
