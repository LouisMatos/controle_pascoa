package br.com.seuprojeto.pascoa.common.quantidade;

import br.com.seuprojeto.pascoa.cadastro.entity.UnidadeVenda;

import java.math.BigDecimal;

public final class Quantidades {

    private static final int CASAS_MAXIMAS = 3;

    private Quantidades() {
    }

    public static void validar(BigDecimal quantidade, UnidadeVenda unidade) {
        if (quantidade == null || quantidade.signum() <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero.");
        }
        int casas = Math.max(quantidade.stripTrailingZeros().scale(), 0);
        if (casas > CASAS_MAXIMAS) {
            throw new IllegalArgumentException("Quantidade aceita no máximo 3 casas decimais.");
        }
        if (casas > 0 && !unidade.isFracionavel()) {
            throw new IllegalArgumentException(unidade.getDescricao() + " só aceita quantidade inteira.");
        }
    }

    public static String formatar(BigDecimal quantidade) {
        if (quantidade == null) {
            return "";
        }
        BigDecimal limpa = quantidade.stripTrailingZeros();
        if (limpa.scale() < 0) {
            limpa = limpa.setScale(0);
        }
        return limpa.toPlainString().replace('.', ',');
    }

    public static String formatar(BigDecimal quantidade, UnidadeVenda unidade) {
        return formatar(quantidade) + " " + unidade.getSimbolo();
    }
}
