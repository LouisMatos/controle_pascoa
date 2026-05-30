package br.com.seuprojeto.pascoa.inventory.adapter.out.persistence;

import br.com.seuprojeto.pascoa.inventory.application.port.out.MovimentacaoRepositoryPort;
import br.com.seuprojeto.pascoa.inventory.domain.model.Movimentacao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MovimentacaoRepositoryAdapter implements MovimentacaoRepositoryPort {

    private final MovimentacaoJpaRepository jpaRepository;
    private final MovimentacaoMapper mapper;

    @Override
    public Movimentacao save(Movimentacao movimentacao) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(movimentacao)));
    }

    @Override
    public List<Movimentacao> findByMateriaPrimaId(Long materiaPrimaId) {
        return jpaRepository.findByMateriaPrimaIdOrderByDataMovimentacaoDesc(materiaPrimaId)
                .stream().map(mapper::toDomain).collect(Collectors.toList());
    }
}
