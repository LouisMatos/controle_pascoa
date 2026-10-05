package br.com.seuprojeto.pascoa.financial.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Getter
@Builder
public class ResumoFinanceiro {

    private final int mes;
    private final int ano;
    private final BigDecimal totalReceitas;
    private final BigDecimal totalDespesas;
    private final BigDecimal lucroLiquido;
    private final BigDecimal margemLucro;   // lucro / receita * 100

    public static ResumoFinanceiro de(int mes, int ano, List<Lancamento> lancamentos) {
        BigDecimal receitas = lancamentos.stream()
                .filter(Lancamento::isReceita)
                .map(Lancamento::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal despesas = lancamentos.stream()
                .filter(Lancamento::isDespesa)
                .map(Lancamento::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal lucro = receitas.subtract(despesas);

        BigDecimal margem = receitas.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                : lucro.divide(receitas, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);

        return ResumoFinanceiro.builder()
                .mes(mes).ano(ano)
                .totalReceitas(receitas)
                .totalDespesas(despesas)
                .lucroLiquido(lucro)
                .margemLucro(margem)
                .build();
    }
}
