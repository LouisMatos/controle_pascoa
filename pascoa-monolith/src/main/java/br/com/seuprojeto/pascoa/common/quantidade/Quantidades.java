package br.com.seuprojeto.pascoa.common.quantidade;

import br.com.seuprojeto.pascoa.cadastro.entity.UnidadeVenda;

import java.math.BigDecimal;

public final class Quantidades {

    private static final int CASAS_MAXIMAS = 3;
    private static final BigDecimal MAXIMO = new BigDecimal("9999999.999");

    public static final BigDecimal TOTAL_MAXIMO = new BigDecimal("99999999.99");

    private Quantidades() {
    }

    public static BigDecimal validarTotal(BigDecimal total) {
        if (total.compareTo(TOTAL_MAXIMO) > 0) {
            throw new IllegalArgumentException("Total acima do máximo permitido.");
        }
        return total;
    }

    public static void validar(BigDecimal quantidade, UnidadeVenda unidade) {
        if (unidade == null) {
            throw new IllegalArgumentException("Unidade de venda é obrigatória.");
        }
        if (quantidade == null || quantidade.signum() <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero.");
        }
        if (quantidade.compareTo(MAXIMO) > 0) {
            throw new IllegalArgumentException("Quantidade acima do máximo permitido.");
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
