package br.com.seuprojeto.pascoa.inventory.adapter.out.persistence;

import br.com.seuprojeto.pascoa.inventory.application.port.out.MateriaPrimaRepositoryPort;
import br.com.seuprojeto.pascoa.inventory.domain.model.MateriaPrima;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MateriaPrimaRepositoryAdapter implements MateriaPrimaRepositoryPort {

    private final MateriaPrimaJpaRepository jpaRepository;
    private final MateriaPrimaMapper mapper;

    @Override
    public Optional<MateriaPrima> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<MateriaPrima> findAllAtivos() {
        return jpaRepository.findAllByAtivoTrue().stream()
                .map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<MateriaPrima> findCriticos() {
        return jpaRepository.findCriticos().stream()
                .map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public MateriaPrima save(MateriaPrima mp) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(mp)));
    }
}
