package br.com.seuprojeto.pascoa.customer.adapter.out.persistence;

import br.com.seuprojeto.pascoa.customer.domain.model.Cliente;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ClienteMapper {

    Cliente toDomain(ClienteJpaEntity entity);

    @Mapping(target = "criadoEm", ignore = true)
    @Mapping(target = "atualizadoEm", ignore = true)
    ClienteJpaEntity toEntity(Cliente domain);
}
