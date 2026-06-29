package br.com.seuprojeto.pascoa.pricing.strategy;

import br.com.seuprojeto.pascoa.pricing.domain.ModeloPreco;
import br.com.seuprojeto.pascoa.pricing.domain.PricingRequest;
import br.com.seuprojeto.pascoa.pricing.domain.PricingResponse;
import br.com.seuprojeto.pascoa.pricing.exception.PricingException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Assinatura recorrente: precoBase × periodos × (1 − descontoFidelidade).
 * descontoFidelidade ∈ [0,1] — ex: 0.05 = 5% off na semanal, 0.12 = 12% off na mensal.
 */
@Component
public class AssinaturaStrategy implements PricingStrategy {
    @Override public ModeloPreco modelo() { return ModeloPreco.ASSINATURA; }

    @Override
    public PricingResponse calcular(PricingRequest r) {
        if (r.precoBase() == null || r.periodos() == null || r.periodos() <= 0)
            throw new PricingException("ASSINATURA requer precoBase e periodos > 0");
        BigDecimal desconto = r.descontoFidelidade() != null ? r.descontoFidelidade() : BigDecimal.ZERO;
        if (desconto.compareTo(BigDecimal.ZERO) < 0 || desconto.compareTo(BigDecimal.ONE) > 0)
            throw new PricingException("descontoFidelidade deve estar entre 0 e 1");
        BigDecimal bruto = r.precoBase().multiply(BigDecimal.valueOf(r.periodos()));
        BigDecimal total = bruto.multiply(BigDecimal.ONE.subtract(desconto))
                .setScale(2, RoundingMode.HALF_UP);
        return new PricingResponse(modelo(), total,
                "precoBase=" + r.precoBase() + " × periodos=" + r.periodos()
                        + " × (1 − " + desconto + ")");
    }
}
