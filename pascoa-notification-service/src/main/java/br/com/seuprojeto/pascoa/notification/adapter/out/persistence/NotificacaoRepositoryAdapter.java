package br.com.seuprojeto.pascoa.notification.adapter.out.persistence;

import br.com.seuprojeto.pascoa.notification.application.port.out.NotificacaoRepositoryPort;
import br.com.seuprojeto.pascoa.notification.domain.model.Notificacao;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class NotificacaoRepositoryAdapter implements NotificacaoRepositoryPort {

    private final NotificacaoJpaRepository jpaRepository;
    private final NotificacaoMapper mapper;

    @Override
    public Notificacao save(Notificacao n) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(n)));
    }

    @Override
    public Optional<Notificacao> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Notificacao> findByReferenciaId(String referenciaId) {
        return jpaRepository.findByReferenciaIdOrderByCriadoEmDesc(referenciaId).stream()
                .map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Notificacao> findRecentes(int limite) {
        return jpaRepository.findAllByOrderByCriadoEmDesc(PageRequest.of(0, limite)).stream()
                .map(mapper::toDomain).collect(Collectors.toList());
    }
}
