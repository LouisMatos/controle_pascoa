package br.com.seuprojeto.pascoa.product.adapter.out.persistence;

import br.com.seuprojeto.pascoa.product.application.port.out.ProdutoRepositoryPort;
import br.com.seuprojeto.pascoa.product.domain.model.Categoria;
import br.com.seuprojeto.pascoa.product.domain.model.Produto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProdutoRepositoryAdapter implements ProdutoRepositoryPort {

    private final ProdutoJpaRepository jpaRepository;
    private final ProdutoMapper mapper;

    @Override
    public Optional<Produto> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Produto> findAllAtivos() {
        return jpaRepository.findAllByAtivoTrue().stream()
                .map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Produto> findDisponiveis() {
        return jpaRepository.findAllByAtivoTrueAndDisponivelTrue().stream()
                .map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Produto> findByCategoria(Categoria categoria) {
        return jpaRepository.findAllByAtivoTrueAndCategoria(categoria).stream()
                .map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public Produto save(Produto produto) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(produto)));
    }
}
