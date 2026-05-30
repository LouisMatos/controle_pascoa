package br.com.seuprojeto.pascoa.auth.adapter.out.persistence;

import br.com.seuprojeto.pascoa.auth.domain.model.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    @Mapping(target = "ativo", source = "ativo")
    Usuario toDomain(UsuarioJpaEntity entity);

    @Mapping(target = "criadoEm", ignore = true)
    @Mapping(target = "atualizadoEm", ignore = true)
    UsuarioJpaEntity toEntity(Usuario domain);
}
