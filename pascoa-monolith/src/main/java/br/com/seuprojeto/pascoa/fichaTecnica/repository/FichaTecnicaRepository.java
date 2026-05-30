package br.com.seuprojeto.pascoa.fichaTecnica.repository;

import br.com.seuprojeto.pascoa.fichaTecnica.entity.FichaTecnica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FichaTecnicaRepository extends JpaRepository<FichaTecnica, Long> {

    Optional<FichaTecnica> findByProdutoId(Long produtoId);

    @Query("SELECT f FROM FichaTecnica f " +
           "LEFT JOIN FETCH f.itens i " +
           "LEFT JOIN FETCH i.materiaPrima " +
           "WHERE f.produto.id = :produtoId")
    Optional<FichaTecnica> findByProdutoIdComItens(@Param("produtoId") Long produtoId);

    @Query("SELECT DISTINCT f FROM FichaTecnica f " +
           "LEFT JOIN FETCH f.itens i " +
           "LEFT JOIN FETCH i.materiaPrima " +
           "LEFT JOIN FETCH f.produto")
    List<FichaTecnica> findAllComItens();
}
