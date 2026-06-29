package br.com.seuprojeto.pascoa.pricing.strategy;

import br.com.seuprojeto.pascoa.pricing.domain.ModeloPreco;
import br.com.seuprojeto.pascoa.pricing.domain.PricingRequest;
import br.com.seuprojeto.pascoa.pricing.domain.PricingResponse;
import br.com.seuprojeto.pascoa.pricing.exception.PricingException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PorUnidadeStrategy implements PricingStrategy {
    @Override public ModeloPreco modelo() { return ModeloPreco.POR_UNIDADE; }

    @Override
    public PricingResponse calcular(PricingRequest r) {
        if (r.precoBase() == null || r.quantidade() == null)
            throw new PricingException("POR_UNIDADE requer precoBase e quantidade");
        BigDecimal total = r.precoBase().multiply(r.quantidade()).setScale(2, RoundingMode.HALF_UP);
        return new PricingResponse(modelo(), total,
                "precoBase=" + r.precoBase() + " × qtd=" + r.quantidade());
    }
}
