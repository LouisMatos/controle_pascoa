package br.com.seuprojeto.pascoa.financial.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class DreAnual {

    private final int ano;
    private final BigDecimal receitaBruta;
    private final BigDecimal custosVariaveis;    // INSUMO + PRODUCAO
    private final BigDecimal lucroBruto;         // receitaBruta - custosVariaveis
    private final BigDecimal despesasOperacionais; // OPERACIONAL + OUTRO
    private final BigDecimal lucroLiquido;       // lucroBruto - despesasOperacionais
    private final List<ResumoFinanceiro> porMes;

    public static DreAnual de(int ano, List<Lancamento> lancamentos, List<ResumoFinanceiro> porMes) {
        BigDecimal receita = soma(lancamentos, TipoLancamento.RECEITA, null);
        BigDecimal custosVar = soma(lancamentos, TipoLancamento.DESPESA, CategoriaLancamento.INSUMO)
                .add(soma(lancamentos, TipoLancamento.DESPESA, CategoriaLancamento.PRODUCAO));
        BigDecimal despOper = soma(lancamentos, TipoLancamento.DESPESA, CategoriaLancamento.OPERACIONAL)
                .add(soma(lancamentos, TipoLancamento.DESPESA, CategoriaLancamento.OUTRO));

        return DreAnual.builder()
                .ano(ano)
                .receitaBruta(receita)
                .custosVariaveis(custosVar)
                .lucroBruto(receita.subtract(custosVar))
                .despesasOperacionais(despOper)
                .lucroLiquido(receita.subtract(custosVar).subtract(despOper))
                .porMes(porMes)
                .build();
    }

    private static BigDecimal soma(List<Lancamento> l, TipoLancamento tipo, CategoriaLancamento cat) {
        return l.stream()
                .filter(x -> x.getTipo() == tipo && (cat == null || x.getCategoria() == cat))
                .map(Lancamento::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
