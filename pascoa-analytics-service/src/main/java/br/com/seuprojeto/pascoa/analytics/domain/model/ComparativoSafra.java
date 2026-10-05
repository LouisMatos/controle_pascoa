package br.com.seuprojeto.pascoa.analytics.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@Builder
public class ComparativoSafra {

    private final MetricaSafra safraAtual;
    private final MetricaSafra safraAnterior;
    private final BigDecimal variacaoReceita;    // % crescimento/queda
    private final BigDecimal variacaoPedidos;
    private final BigDecimal variacaoClientes;

    public static ComparativoSafra de(MetricaSafra atual, MetricaSafra anterior) {
        return ComparativoSafra.builder()
                .safraAtual(atual)
                .safraAnterior(anterior)
                .variacaoReceita(variacao(atual.getTotalReceita(), anterior.getTotalReceita()))
                .variacaoPedidos(variacao(BigDecimal.valueOf(atual.getTotalPedidos()),
                        BigDecimal.valueOf(anterior.getTotalPedidos())))
                .variacaoClientes(variacao(BigDecimal.valueOf(atual.getTotalClientes()),
                        BigDecimal.valueOf(anterior.getTotalClientes())))
                .build();
    }

    private static BigDecimal variacao(BigDecimal atual, BigDecimal anterior) {
        if (anterior.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return atual.subtract(anterior)
                .divide(anterior, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
