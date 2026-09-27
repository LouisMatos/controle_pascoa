package br.com.seuprojeto.pascoa.product.adapter.out.persistence;

import br.com.seuprojeto.pascoa.product.domain.model.Produto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProdutoMapper {
    Produto toDomain(ProdutoJpaEntity entity);

    @Mapping(target = "criadoEm", ignore = true)
    @Mapping(target = "atualizadoEm", ignore = true)
    ProdutoJpaEntity toEntity(Produto domain);
}
