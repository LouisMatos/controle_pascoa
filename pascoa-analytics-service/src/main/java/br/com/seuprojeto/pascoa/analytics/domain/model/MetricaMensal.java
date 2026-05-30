package br.com.seuprojeto.pascoa.analytics.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class MetricaMensal {

    private final int mes;
    private final int ano;
    private final long totalPedidos;
    private final BigDecimal totalReceita;
    private final long totalClientes;

    public static MetricaMensal de(int mes, int ano, List<RegistroVenda> registros) {
        List<RegistroVenda> doMes = registros.stream()
                .filter(r -> r.getMes() == mes && r.getAno() == ano)
                .toList();
        long pedidos  = doMes.stream().map(RegistroVenda::getPedidoId).distinct().count();
        long clientes = doMes.stream().map(RegistroVenda::getClienteId).distinct().count();
        BigDecimal receita = doMes.stream()
                .map(RegistroVenda::getValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return MetricaMensal.builder()
                .mes(mes).ano(ano)
                .totalPedidos(pedidos)
                .totalReceita(receita)
                .totalClientes(clientes)
                .build();
    }
}
