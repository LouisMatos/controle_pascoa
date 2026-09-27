package br.com.seuprojeto.pascoa.order.domain.model;

import br.com.seuprojeto.pascoa.order.domain.exception.TransicaoInvalidaException;
import lombok.Builder;
import lombok.Getter;
import lombok.With;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@With
public class Pedido {

    private final Long id;
    private final Long clienteId;
    private final String nomeCliente;
    private final StatusPedido status;
    @Builder.Default
    private final List<ItemPedido> itens = new ArrayList<>();
    private final FormaPagamento formaPagamento;
    private final String observacao;
    private final String tokenRastreamento;
    private final LocalDateTime criadoEm;
    private final LocalDateTime atualizadoEm;

    public static Pedido novo(Long clienteId, String nomeCliente, String observacao) {
        return Pedido.builder()
                .clienteId(clienteId)
                .nomeCliente(nomeCliente)
                .status(StatusPedido.NOVO)
                .observacao(observacao)
                .tokenRastreamento(UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase())
                .criadoEm(LocalDateTime.now())
                .atualizadoEm(LocalDateTime.now())
                .build();
    }

    public Pedido transicionarPara(StatusPedido novoStatus) {
        if (!status.podeTransicionarPara(novoStatus)) {
            throw new TransicaoInvalidaException(id, status, novoStatus);
        }
        return this.withStatus(novoStatus).withAtualizadoEm(LocalDateTime.now());
    }

    public BigDecimal total() {
        return itens.stream()
                .map(ItemPedido::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean estaFinalizado() {
        return status == StatusPedido.ENTREGUE || status == StatusPedido.CANCELADO;
    }

    public List<ItemPedido> getItens() {
        return Collections.unmodifiableList(itens);
    }
}
