package br.com.seuprojeto.pascoa.financial.adapter.out.persistence;

import br.com.seuprojeto.pascoa.financial.domain.model.Lancamento;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LancamentoMapper {
    Lancamento toDomain(LancamentoJpaEntity entity);

    @Mapping(target = "criadoEm", ignore = true)
    LancamentoJpaEntity toEntity(Lancamento domain);
}
