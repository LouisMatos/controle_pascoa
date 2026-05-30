package br.com.seuprojeto.pascoa.order.adapter.out.persistence;

import br.com.seuprojeto.pascoa.order.application.port.out.PedidoRepositoryPort;
import br.com.seuprojeto.pascoa.order.domain.model.ItemPedido;
import br.com.seuprojeto.pascoa.order.domain.model.Pedido;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PedidoRepositoryAdapter implements PedidoRepositoryPort {

    private final PedidoJpaRepository jpaRepository;

    @Override
    public Optional<Pedido> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Pedido> findByToken(String token) {
        return jpaRepository.findByTokenRastreamento(token).map(this::toDomain);
    }

    @Override
    public List<Pedido> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Pedido save(Pedido pedido) {
        PedidoJpaEntity entity = toEntity(pedido);
        return toDomain(jpaRepository.save(entity));
    }

    private Pedido toDomain(PedidoJpaEntity e) {
        List<ItemPedido> itens = e.getItens().stream()
                .map(i -> ItemPedido.builder()
                        .id(i.getId())
                        .produtoId(i.getProdutoId())
                        .nomeProduto(i.getNomeProduto())
                        .precoUnitario(i.getPrecoUnitario())
                        .quantidade(i.getQuantidade())
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));

        return Pedido.builder()
                .id(e.getId())
                .clienteId(e.getClienteId())
                .nomeCliente(e.getNomeCliente())
                .status(e.getStatus())
                .itens(itens)
                .formaPagamento(e.getFormaPagamento())
                .observacao(e.getObservacao())
                .tokenRastreamento(e.getTokenRastreamento())
                .criadoEm(e.getCriadoEm())
                .atualizadoEm(e.getAtualizadoEm())
                .build();
    }

    private PedidoJpaEntity toEntity(Pedido p) {
        PedidoJpaEntity entity = new PedidoJpaEntity();
        entity.setId(p.getId());
        entity.setClienteId(p.getClienteId());
        entity.setNomeCliente(p.getNomeCliente());
        entity.setStatus(p.getStatus());
        entity.setFormaPagamento(p.getFormaPagamento());
        entity.setObservacao(p.getObservacao());
        entity.setTokenRastreamento(p.getTokenRastreamento());

        entity.getItens().clear();
        p.getItens().forEach(item -> {
            ItemPedidoJpaEntity ie = new ItemPedidoJpaEntity();
            ie.setId(item.getId());
            ie.setPedido(entity);
            ie.setProdutoId(item.getProdutoId());
            ie.setNomeProduto(item.getNomeProduto());
            ie.setPrecoUnitario(item.getPrecoUnitario());
            ie.setQuantidade(item.getQuantidade());
            entity.getItens().add(ie);
        });

        return entity;
    }
}
