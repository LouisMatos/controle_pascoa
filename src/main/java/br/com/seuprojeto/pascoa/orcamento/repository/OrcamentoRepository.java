package br.com.seuprojeto.pascoa.orcamento.repository;

import br.com.seuprojeto.pascoa.orcamento.entity.Orcamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface OrcamentoRepository extends JpaRepository<Orcamento, Long> {

    @Query("SELECT o FROM Orcamento o JOIN FETCH o.cliente ORDER BY o.dataCriacao DESC")
    List<Orcamento> findAllComCliente();

    @Query("SELECT o FROM Orcamento o JOIN FETCH o.cliente JOIN FETCH o.itens i JOIN FETCH i.produto WHERE o.id = :id")
    Optional<Orcamento> findByIdComItens(Long id);

    Optional<Orcamento> findByTokenAprovacao(String token);
}
