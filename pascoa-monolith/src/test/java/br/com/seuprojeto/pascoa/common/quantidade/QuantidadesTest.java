package br.com.seuprojeto.pascoa.common.quantidade;

import br.com.seuprojeto.pascoa.cadastro.entity.UnidadeVenda;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuantidadesTest {

    @Test
    void inteiroEhValidoParaTodasAsUnidades() {
        for (UnidadeVenda un : UnidadeVenda.values()) {
            assertThatCode(() -> Quantidades.validar(new BigDecimal("2"), un)).doesNotThrowAnyException();
            assertThatCode(() -> Quantidades.validar(new BigDecimal("2.000"), un)).doesNotThrowAnyException();
        }
    }

    @Test
    void fracaoSoParaKg() {
        assertThatCode(() -> Quantidades.validar(new BigDecimal("1.5"), UnidadeVenda.KG)).doesNotThrowAnyException();
        assertThatCode(() -> Quantidades.validar(new BigDecimal("0.001"), UnidadeVenda.KG)).doesNotThrowAnyException();
        assertThatThrownBy(() -> Quantidades.validar(new BigDecimal("1.5"), UnidadeVenda.CENTO))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cento só aceita quantidade inteira.");
    }

    @Test
    void zeroNegativoENuloSaoRejeitados() {
        assertThatThrownBy(() -> Quantidades.validar(BigDecimal.ZERO, UnidadeVenda.KG))
            .hasMessage("Quantidade deve ser maior que zero.");
        assertThatThrownBy(() -> Quantidades.validar(new BigDecimal("-1"), UnidadeVenda.UNIDADE))
            .hasMessage("Quantidade deve ser maior que zero.");
        assertThatThrownBy(() -> Quantidades.validar(null, UnidadeVenda.UNIDADE))
            .hasMessage("Quantidade deve ser maior que zero.");
    }

    @Test
    void limiteMaximoEhAceito_eAcimaEhRejeitado() {
        assertThatCode(() -> Quantidades.validar(new BigDecimal("9999999.999"), UnidadeVenda.KG)).doesNotThrowAnyException();
        assertThatThrownBy(() -> Quantidades.validar(new BigDecimal("10000000"), UnidadeVenda.KG))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Quantidade acima do máximo permitido.");
    }

    @Test
    void unidadeNulaEhRejeitada() {
        assertThatThrownBy(() -> Quantidades.validar(BigDecimal.ONE, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unidade de venda é obrigatória.");
    }

    @Test
    void maisDeTresCasasEhRejeitado() {
        assertThatThrownBy(() -> Quantidades.validar(new BigDecimal("1.2345"), UnidadeVenda.KG))
            .hasMessage("Quantidade aceita no máximo 3 casas decimais.");
    }

    @Test
    void formatar_naoMostraZerosAEsquerdaDaVirgulaNemADireita() {
        assertThat(Quantidades.formatar(new BigDecimal("2.000"))).isEqualTo("2");
        assertThat(Quantidades.formatar(new BigDecimal("1.500"))).isEqualTo("1,5");
        assertThat(Quantidades.formatar(new BigDecimal("0.250"))).isEqualTo("0,25");
        assertThat(Quantidades.formatar(new BigDecimal("100"))).isEqualTo("100");
        assertThat(Quantidades.formatar(new BigDecimal("1E+2"))).isEqualTo("100");
        assertThat(Quantidades.formatar(null)).isEmpty();
    }

    @Test
    void formatar_comUnidade() {
        assertThat(Quantidades.formatar(new BigDecimal("2.000"), UnidadeVenda.CENTO)).isEqualTo("2 cento");
        assertThat(Quantidades.formatar(new BigDecimal("1.5"), UnidadeVenda.KG)).isEqualTo("1,5 kg");
    }
}
