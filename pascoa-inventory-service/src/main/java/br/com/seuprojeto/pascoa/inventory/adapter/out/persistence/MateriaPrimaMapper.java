package br.com.seuprojeto.pascoa.inventory.adapter.out.persistence;

import br.com.seuprojeto.pascoa.inventory.domain.model.MateriaPrima;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MateriaPrimaMapper {
    MateriaPrima toDomain(MateriaPrimaJpaEntity entity);

    @Mapping(target = "criadoEm", ignore = true)
    @Mapping(target = "atualizadoEm", ignore = true)
    MateriaPrimaJpaEntity toEntity(MateriaPrima domain);
}
