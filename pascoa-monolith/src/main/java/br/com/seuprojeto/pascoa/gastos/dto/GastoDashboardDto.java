package br.com.seuprojeto.pascoa.gastos.dto;

import br.com.seuprojeto.pascoa.gastos.entity.CategoriaGasto;

import java.math.BigDecimal;
import java.util.List;

public record GastoDashboardDto(
        int mes,
        int ano,
        BigDecimal totalRealizado,
        BigDecimal totalOrcado,
        List<CategoriaResumo> porCategoria
) {
    public record CategoriaResumo(
            CategoriaGasto categoria,
            BigDecimal realizado,
            BigDecimal orcado,
            BigDecimal saldo
    ) {
        public boolean estourado() {
            return orcado.compareTo(BigDecimal.ZERO) > 0
                    && realizado.compareTo(orcado) > 0;
        }

        public int percentualUtilizado() {
            if (orcado.compareTo(BigDecimal.ZERO) <= 0) return 0;
            return realizado.multiply(BigDecimal.valueOf(100))
                    .divide(orcado, 0, java.math.RoundingMode.HALF_UP)
                    .intValue();
        }
    }
}
