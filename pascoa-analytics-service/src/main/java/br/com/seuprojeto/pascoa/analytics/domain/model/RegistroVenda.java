package br.com.seuprojeto.pascoa.analytics.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class RegistroVenda {
    private final Long id;
    private final Long pedidoId;
    private final Long clienteId;
    private final Long produtoId;
    private final String nomeProduto;
    private final int quantidade;
    private final BigDecimal valorTotal;
    private final LocalDate dataVenda;
    private final int ano;
    private final int mes;
}
