package br.com.seuprojeto.pascoa.pricing.strategy;

import br.com.seuprojeto.pascoa.pricing.domain.ModeloPreco;
import br.com.seuprojeto.pascoa.pricing.domain.PricingRequest;
import br.com.seuprojeto.pascoa.pricing.domain.PricingResponse;
import br.com.seuprojeto.pascoa.pricing.exception.PricingException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PorPesoStrategy implements PricingStrategy {
    @Override public ModeloPreco modelo() { return ModeloPreco.POR_PESO; }

    @Override
    public PricingResponse calcular(PricingRequest r) {
        if (r.precoBase() == null || r.pesoKg() == null)
            throw new PricingException("POR_PESO requer precoBase (R$/kg) e pesoKg");
        BigDecimal total = r.precoBase().multiply(r.pesoKg()).setScale(2, RoundingMode.HALF_UP);
        return new PricingResponse(modelo(), total,
                "precoBase=R$" + r.precoBase() + "/kg × peso=" + r.pesoKg() + "kg");
    }
}
