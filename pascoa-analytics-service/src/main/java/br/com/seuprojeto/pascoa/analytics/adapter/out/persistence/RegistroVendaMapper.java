package br.com.seuprojeto.pascoa.analytics.adapter.out.persistence;

import br.com.seuprojeto.pascoa.analytics.domain.model.RegistroVenda;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RegistroVendaMapper {
    RegistroVenda toDomain(RegistroVendaJpaEntity entity);

    @Mapping(target = "criadoEm", ignore = true)
    RegistroVendaJpaEntity toEntity(RegistroVenda domain);
}
