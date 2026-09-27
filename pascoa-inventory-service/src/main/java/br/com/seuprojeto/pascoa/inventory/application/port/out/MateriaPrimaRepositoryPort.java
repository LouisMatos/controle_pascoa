package br.com.seuprojeto.pascoa.inventory.application.port.out;

import br.com.seuprojeto.pascoa.inventory.domain.model.MateriaPrima;

import java.util.List;
import java.util.Optional;

public interface MateriaPrimaRepositoryPort {
    Optional<MateriaPrima> findById(Long id);
    List<MateriaPrima> findAllAtivos();
    List<MateriaPrima> findCriticos();
    MateriaPrima save(MateriaPrima materiaPrima);
}
