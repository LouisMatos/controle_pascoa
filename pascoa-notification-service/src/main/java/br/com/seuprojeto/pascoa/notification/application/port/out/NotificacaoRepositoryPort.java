package br.com.seuprojeto.pascoa.notification.application.port.out;

import br.com.seuprojeto.pascoa.notification.domain.model.Notificacao;

import java.util.List;
import java.util.Optional;

public interface NotificacaoRepositoryPort {
    Notificacao save(Notificacao notificacao);
    Optional<Notificacao> findById(Long id);
    List<Notificacao> findByReferenciaId(String referenciaId);
    List<Notificacao> findRecentes(int limite);
}
