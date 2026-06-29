package br.com.seuprojeto.pascoa.pricing.strategy;

import br.com.seuprojeto.pascoa.pricing.domain.ModeloPreco;
import br.com.seuprojeto.pascoa.pricing.domain.PricingRequest;
import br.com.seuprojeto.pascoa.pricing.domain.PricingResponse;
import br.com.seuprojeto.pascoa.pricing.exception.PricingException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PorComboStrategy implements PricingStrategy {
    @Override public ModeloPreco modelo() { return ModeloPreco.POR_COMBO; }

    @Override
    public PricingResponse calcular(PricingRequest r) {
        if (r.itensCombo() == null || r.itensCombo().isEmpty())
            throw new PricingException("POR_COMBO requer ao menos 1 item em itensCombo");
        BigDecimal soma = BigDecimal.ZERO;
        for (var it : r.itensCombo()) {
            soma = soma.add(it.precoItem().multiply(it.qtd()));
        }
        BigDecimal desconto = r.descontoCombo() != null ? r.descontoCombo() : BigDecimal.ZERO;
        BigDecimal total = soma.subtract(desconto).max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        return new PricingResponse(modelo(), total,
                "soma=" + soma + " − desconto=" + desconto);
    }
}
