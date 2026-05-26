package br.com.seuprojeto.pascoa.analytics.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public record ComparativoSafraDto(List<SafraDto> safras) {

    /** Variação percentual de faturamento entre o último e o penúltimo ano disponíveis. */
    public BigDecimal variacaoFaturamento() {
        if (safras.size() < 2) return BigDecimal.ZERO;
        BigDecimal atual = safras.get(0).totalFaturamento();
        BigDecimal anterior = safras.get(1).totalFaturamento();
        if (anterior.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return atual.subtract(anterior)
                    .divide(anterior, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(1, RoundingMode.HALF_UP);
    }
}
