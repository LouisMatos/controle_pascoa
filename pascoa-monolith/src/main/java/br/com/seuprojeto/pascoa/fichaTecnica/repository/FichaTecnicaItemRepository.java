package br.com.seuprojeto.pascoa.fichaTecnica.repository;

import br.com.seuprojeto.pascoa.fichaTecnica.entity.FichaTecnicaItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FichaTecnicaItemRepository extends JpaRepository<FichaTecnicaItem, Long> {

    boolean existsByFichaTecnicaIdAndMateriaPrimaId(Long fichaTecnicaId, Long materiaPrimaId);
}
