package br.com.seuprojeto.pascoa.analytics.adapter.out.persistence;

import br.com.seuprojeto.pascoa.analytics.application.port.out.RegistroVendaRepositoryPort;
import br.com.seuprojeto.pascoa.analytics.domain.model.RegistroVenda;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RegistroVendaRepositoryAdapter implements RegistroVendaRepositoryPort {

    private final RegistroVendaJpaRepository jpaRepository;
    private final RegistroVendaMapper mapper;

    @Override
    public RegistroVenda save(RegistroVenda registro) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(registro)));
    }

    @Override
    public List<RegistroVenda> findByAno(int ano) {
        return jpaRepository.findByAno(ano).stream()
                .map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public boolean existsByPedidoId(Long pedidoId) {
        return jpaRepository.existsByPedidoId(pedidoId);
    }
}
