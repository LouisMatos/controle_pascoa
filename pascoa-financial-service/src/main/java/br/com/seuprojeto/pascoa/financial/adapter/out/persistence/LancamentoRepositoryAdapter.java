package br.com.seuprojeto.pascoa.financial.adapter.out.persistence;

import br.com.seuprojeto.pascoa.financial.application.port.out.LancamentoRepositoryPort;
import br.com.seuprojeto.pascoa.financial.domain.model.Lancamento;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class LancamentoRepositoryAdapter implements LancamentoRepositoryPort {

    private final LancamentoJpaRepository jpaRepository;
    private final LancamentoMapper mapper;

    @Override
    public Lancamento save(Lancamento l) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(l)));
    }

    @Override
    public Optional<Lancamento> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Lancamento> findByMesAno(int mes, int ano) {
        return jpaRepository.findByMesAno(mes, ano).stream()
                .map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Lancamento> findByPeriodo(LocalDate inicio, LocalDate fim) {
        return jpaRepository.findByDataBetweenOrderByData(inicio, fim).stream()
                .map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Lancamento> findByAno(int ano) {
        return jpaRepository.findByAno(ano).stream()
                .map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public boolean existsByReferenciaIdAndOrigem(String referenciaId, String origem) {
        return jpaRepository.existsByReferenciaIdAndOrigem(referenciaId, origem);
    }
}
