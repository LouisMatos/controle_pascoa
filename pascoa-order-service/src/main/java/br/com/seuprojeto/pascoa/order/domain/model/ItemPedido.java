package br.com.seuprojeto.pascoa.order.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ItemPedido {
    private final Long id;
    private final Long produtoId;
    private final String nomeProduto;
    private final BigDecimal precoUnitario;
    private final int quantidade;

    public BigDecimal subtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }
}
