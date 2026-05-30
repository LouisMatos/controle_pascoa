package br.com.seuprojeto.pascoa.notification.adapter.out.persistence;

import br.com.seuprojeto.pascoa.notification.domain.model.Notificacao;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificacaoMapper {
    Notificacao toDomain(NotificacaoJpaEntity entity);
    NotificacaoJpaEntity toEntity(Notificacao domain);
}
