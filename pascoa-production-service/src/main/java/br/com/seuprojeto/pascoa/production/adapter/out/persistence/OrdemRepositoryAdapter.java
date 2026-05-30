package br.com.seuprojeto.pascoa.production.adapter.out.persistence;

import br.com.seuprojeto.pascoa.production.application.port.out.OrdemRepositoryPort;
import br.com.seuprojeto.pascoa.production.domain.model.ItemOrdem;
import br.com.seuprojeto.pascoa.production.domain.model.OrdemProducao;
import br.com.seuprojeto.pascoa.production.domain.model.StatusOrdem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrdemRepositoryAdapter implements OrdemRepositoryPort {

    private final OrdemJpaRepository jpaRepository;

    @Override
    public Optional<OrdemProducao> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<OrdemProducao> findByPedidoId(Long pedidoId) {
        return jpaRepository.findByPedidoId(pedidoId).map(this::toDomain);
    }

    @Override
    public List<OrdemProducao> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<OrdemProducao> findByStatus(StatusOrdem status) {
        return jpaRepository.findByStatus(status).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public OrdemProducao save(OrdemProducao ordem) {
        return toDomain(jpaRepository.save(toEntity(ordem)));
    }

    private OrdemProducao toDomain(OrdemProducaoJpaEntity e) {
        List<ItemOrdem> itens = e.getItens().stream()
                .map(i -> ItemOrdem.builder()
                        .produtoId(i.getProdutoId())
                        .nomeProduto(i.getNomeProduto())
                        .quantidade(i.getQuantidade())
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));

        return OrdemProducao.builder()
                .id(e.getId())
                .pedidoId(e.getPedidoId())
                .nomeCliente(e.getNomeCliente())
                .status(e.getStatus())
                .itens(itens)
                .dataPrevisao(e.getDataPrevisao())
                .observacoes(e.getObservacoes())
                .criadoEm(e.getCriadoEm())
                .atualizadoEm(e.getAtualizadoEm())
                .build();
    }

    private OrdemProducaoJpaEntity toEntity(OrdemProducao o) {
        OrdemProducaoJpaEntity entity = new OrdemProducaoJpaEntity();
        entity.setId(o.getId());
        entity.setPedidoId(o.getPedidoId());
        entity.setNomeCliente(o.getNomeCliente());
        entity.setStatus(o.getStatus());
        entity.setDataPrevisao(o.getDataPrevisao());
        entity.setObservacoes(o.getObservacoes());

        entity.getItens().clear();
        if (o.getItens() != null) {
            o.getItens().forEach(item -> {
                ItemOrdemJpaEntity ie = new ItemOrdemJpaEntity();
                ie.setOrdem(entity);
                ie.setProdutoId(item.getProdutoId());
                ie.setNomeProduto(item.getNomeProduto());
                ie.setQuantidade(item.getQuantidade());
                entity.getItens().add(ie);
            });
        }
        return entity;
    }
}
