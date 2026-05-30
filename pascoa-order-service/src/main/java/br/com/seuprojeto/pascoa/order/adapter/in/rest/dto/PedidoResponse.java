package br.com.seuprojeto.pascoa.order.adapter.in.rest.dto;

import br.com.seuprojeto.pascoa.order.domain.model.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponse(
        Long id,
        Long clienteId,
        String nomeCliente,
        StatusPedido status,
        List<ItemResponse> itens,
        BigDecimal total,
        FormaPagamento formaPagamento,
        String observacao,
        String tokenRastreamento,
        LocalDateTime criadoEm
) {
    public record ItemResponse(Long id, Long produtoId, String nomeProduto,
                               BigDecimal precoUnitario, int quantidade, BigDecimal subtotal) {}

    public static PedidoResponse from(Pedido p) {
        var itens = p.getItens().stream()
                .map(i -> new ItemResponse(i.getId(), i.getProdutoId(), i.getNomeProduto(),
                        i.getPrecoUnitario(), i.getQuantidade(), i.subtotal()))
                .toList();
        return new PedidoResponse(p.getId(), p.getClienteId(), p.getNomeCliente(),
                p.getStatus(), itens, p.total(), p.getFormaPagamento(),
                p.getObservacao(), p.getTokenRastreamento(), p.getCriadoEm());
    }
}
