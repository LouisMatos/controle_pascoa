package br.com.seuprojeto.pascoa.pricing.strategy;

import br.com.seuprojeto.pascoa.pricing.domain.ModeloPreco;
import br.com.seuprojeto.pascoa.pricing.domain.PricingRequest;
import br.com.seuprojeto.pascoa.pricing.domain.PricingResponse;
import br.com.seuprojeto.pascoa.pricing.exception.PricingException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Avalia uma expressão SpEL configurada pelo tenant. Exemplo do design v6 §5.3:
 * <pre>{@code
 * formula = "(raio * raio * 3.14 * camadas * 0.008) * fatorRecheio * fatorCobertura"
 * variaveis = { raio: 12.5, camadas: 3, fatorRecheio: 1.3, fatorCobertura: 1.6 }
 * }</pre>
 *
 * <p>Por segurança usa {@link SimpleEvaluationContext} (read-only, sem chamadas a métodos
 * arbitrários) — apenas propriedades e operações aritméticas.
 */
@Component
public class FormulaCustomStrategy implements PricingStrategy {

    private final ExpressionParser parser =
            new SpelExpressionParser(new SpelParserConfiguration(false, false));

    @Override public ModeloPreco modelo() { return ModeloPreco.FORMULA_CUSTOM; }

    @Override
    public PricingResponse calcular(PricingRequest r) {
        if (r.formula() == null || r.formula().isBlank())
            throw new PricingException("FORMULA_CUSTOM requer campo 'formula'");
        EvaluationContext ctx = SimpleEvaluationContext.forReadOnlyDataBinding().build();
        if (r.variaveis() != null) {
            r.variaveis().forEach(ctx::setVariable);
        }
        // SpEL acessa variáveis com #nome; permitimos os dois estilos transformando "raio" → "#raio"
        // se o usuário não tiver colocado o '#'. Substituição simples por palavra.
        String expr = normalizarVariaveis(r.formula(), r.variaveis() != null ? r.variaveis().keySet() : java.util.Set.of());

        try {
            Number valor = parser.parseExpression(expr).getValue(ctx, Number.class);
            if (valor == null) throw new PricingException("Fórmula resultou em null: " + expr);
            BigDecimal total = new BigDecimal(valor.toString()).setScale(2, RoundingMode.HALF_UP);
            return new PricingResponse(modelo(), total, "formula=" + r.formula());
        } catch (SpelEvaluationException e) {
            throw new PricingException("Erro avaliando fórmula: " + e.getMessage(), e);
        }
    }

    private String normalizarVariaveis(String expr, java.util.Set<String> chaves) {
        String out = expr;
        for (String chave : chaves) {
            // só substitui ocorrências que não estão precedidas por '#' (já marcadas) nem por letra
            out = out.replaceAll("(?<![#A-Za-z_0-9])" + java.util.regex.Pattern.quote(chave) + "\\b",
                    "#" + chave);
        }
        return out;
    }
}
