package br.com.seuprojeto.pascoa.analytics.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Getter
@Builder
public class MetricaSafra {

    private final int ano;
    private final long totalPedidos;
    private final BigDecimal totalReceita;
    private final long totalClientes;
    private final BigDecimal ticketMedio;

    public static MetricaSafra de(int ano, List<RegistroVenda> registros) {
        long pedidos   = registros.stream().map(RegistroVenda::getPedidoId).distinct().count();
        long clientes  = registros.stream().map(RegistroVenda::getClienteId).distinct().count();
        BigDecimal receita = registros.stream()
                .map(RegistroVenda::getValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal ticket = pedidos == 0 ? BigDecimal.ZERO
                : receita.divide(BigDecimal.valueOf(pedidos), 2, RoundingMode.HALF_UP);

        return MetricaSafra.builder()
                .ano(ano).totalPedidos(pedidos).totalReceita(receita)
                .totalClientes(clientes).ticketMedio(ticket)
                .build();
    }
}
