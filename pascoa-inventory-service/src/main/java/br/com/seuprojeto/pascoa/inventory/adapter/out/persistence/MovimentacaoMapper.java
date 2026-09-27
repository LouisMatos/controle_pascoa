package br.com.seuprojeto.pascoa.inventory.adapter.out.persistence;

import br.com.seuprojeto.pascoa.inventory.domain.model.Movimentacao;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MovimentacaoMapper {
    Movimentacao toDomain(MovimentacaoJpaEntity entity);
    MovimentacaoJpaEntity toEntity(Movimentacao domain);
}
