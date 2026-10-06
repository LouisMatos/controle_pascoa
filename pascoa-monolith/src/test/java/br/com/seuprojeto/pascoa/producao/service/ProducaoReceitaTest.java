package br.com.seuprojeto.pascoa.producao.service;

import br.com.seuprojeto.pascoa.cadastro.entity.MateriaPrima;
import br.com.seuprojeto.pascoa.cadastro.entity.Unidade;
import br.com.seuprojeto.pascoa.fichaTecnica.entity.FichaTecnica;
import br.com.seuprojeto.pascoa.fichaTecnica.entity.FichaTecnicaItem;
import br.com.seuprojeto.pascoa.producao.entity.OrdemProducao;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProducaoReceitaTest {

    private final ProducaoService service = new ProducaoService(null, null, null, null, null);

    @Test
    void calcularReceita_escalaPelaOrdemEMultiplicaCusto() {
        MateriaPrima comCusto = MateriaPrima.builder().nome("Chocolate").unidade(Unidade.KG)
            .quantidadeAtual(new BigDecimal("3")).custoUnitario(new BigDecimal("10")).build();
        MateriaPrima semCusto = MateriaPrima.builder().nome("Leite").unidade(Unidade.KG)
            .quantidadeAtual(new BigDecimal("100")).build();
        FichaTecnica ficha = FichaTecnica.builder().rendimento(new BigDecimal("5")).itens(List.of(
            FichaTecnicaItem.builder().materiaPrima(comCusto).quantidade(new BigDecimal("2")).build(),
            FichaTecnicaItem.builder().materiaPrima(semCusto).quantidade(new BigDecimal("1")).build())).build();
        OrdemProducao ordem = OrdemProducao.builder().quantidade(BigDecimal.TEN).build();

        var r = service.calcularReceita(ordem, ficha);

        assertThat(r.linhas().get(0).qtdNecessaria()).isEqualByComparingTo("4.000");
        assertThat(r.linhas().get(0).custo()).isEqualByComparingTo("40.00");
        assertThat(r.linhas().get(0).estoqueOk()).isFalse();
        assertThat(r.custoTotal()).isEqualByComparingTo("40.00");
        assertThat(r.custoPorUnidade()).isEqualByComparingTo("4.00");
        assertThat(r.insuficientes()).isEqualTo(1);
        assertThat(r.semCusto()).isEqualTo(1);
    }

    @Test
    void calcularReceita_aceitaQuantidadeFracionada() {
        MateriaPrima comCusto = MateriaPrima.builder().nome("Chocolate").unidade(Unidade.KG)
            .quantidadeAtual(new BigDecimal("3")).custoUnitario(new BigDecimal("10")).build();
        FichaTecnica ficha = FichaTecnica.builder().rendimento(new BigDecimal("5")).itens(List.of(
            FichaTecnicaItem.builder().materiaPrima(comCusto).quantidade(new BigDecimal("2")).build())).build();
        OrdemProducao ordem = OrdemProducao.builder().quantidade(new BigDecimal("2.5")).build();

        var r = service.calcularReceita(ordem, ficha);

        assertThat(r.linhas().get(0).qtdNecessaria()).isEqualByComparingTo("1.000");
        assertThat(r.linhas().get(0).custo()).isEqualByComparingTo("10.00");
        assertThat(r.linhas().get(0).estoqueOk()).isTrue();
        assertThat(r.custoPorUnidade()).isEqualByComparingTo("4.00");
    }
}
