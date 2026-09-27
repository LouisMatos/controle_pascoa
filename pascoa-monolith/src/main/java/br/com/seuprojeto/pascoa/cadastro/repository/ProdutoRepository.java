package br.com.seuprojeto.pascoa.cadastro.repository;

import br.com.seuprojeto.pascoa.cadastro.entity.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    @Query("SELECT p FROM Produto p WHERE p.excluidoEm IS NULL ORDER BY p.nome ASC")
    List<Produto> findAllByOrderByNomeAsc();

    @Query("SELECT p FROM Produto p WHERE p.excluidoEm IS NULL AND p.ativo = true ORDER BY p.nome ASC")
    List<Produto> findByAtivoTrueOrderByNomeAsc();

    @Query("SELECT p FROM Produto p WHERE p.excluidoEm IS NULL "
         + "AND LOWER(p.nome) LIKE LOWER(CONCAT('%', :nome, '%')) ORDER BY p.nome ASC")
    List<Produto> findByNomeContainingIgnoreCaseOrderByNomeAsc(@Param("nome") String nome);

    @Query("SELECT p FROM Produto p WHERE p.excluidoEm IS NULL AND p.id IN :ids")
    List<Produto> findVigentesByIds(@Param("ids") List<Long> ids);

    @Query("SELECT p FROM Produto p WHERE p.excluidoEm IS NULL AND p.id = :id")
    java.util.Optional<Produto> findVigenteById(@Param("id") Long id);
}
