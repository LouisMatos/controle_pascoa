package br.com.seuprojeto.pascoa.cadastro;

import br.com.seuprojeto.pascoa.cadastro.entity.UnidadeVenda;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UnidadeVendaTest {

    @Test
    void somenteKgAceitaFracao() {
        assertThat(UnidadeVenda.KG.isFracionavel()).isTrue();
        assertThat(UnidadeVenda.UNIDADE.isFracionavel()).isFalse();
        assertThat(UnidadeVenda.DUZIA.isFracionavel()).isFalse();
        assertThat(UnidadeVenda.CENTO.isFracionavel()).isFalse();
        assertThat(UnidadeVenda.PACOTE.isFracionavel()).isFalse();
    }

    @Test
    void simbolosParaExibicao() {
        assertThat(UnidadeVenda.CENTO.getSimbolo()).isEqualTo("cento");
        assertThat(UnidadeVenda.DUZIA.getSimbolo()).isEqualTo("dz");
        assertThat(UnidadeVenda.KG.getSimbolo()).isEqualTo("kg");
    }
}
