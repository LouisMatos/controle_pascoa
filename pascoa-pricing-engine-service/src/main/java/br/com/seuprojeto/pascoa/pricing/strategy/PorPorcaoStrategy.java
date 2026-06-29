package br.com.seuprojeto.pascoa.pricing.strategy;

import br.com.seuprojeto.pascoa.pricing.domain.ModeloPreco;
import br.com.seuprojeto.pascoa.pricing.domain.PricingRequest;
import br.com.seuprojeto.pascoa.pricing.domain.PricingResponse;
import br.com.seuprojeto.pascoa.pricing.exception.PricingException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PorPorcaoStrategy implements PricingStrategy {
    @Override public ModeloPreco modelo() { return ModeloPreco.POR_PORCAO; }

    @Override
    public PricingResponse calcular(PricingRequest r) {
        if (r.precoBase() == null || r.qtdPorcoes() == null || r.qtdPorcoes() <= 0)
            throw new PricingException("POR_PORCAO requer precoBase e qtdPorcoes > 0");
        BigDecimal total = r.precoBase().multiply(BigDecimal.valueOf(r.qtdPorcoes()))
                .setScale(2, RoundingMode.HALF_UP);
        return new PricingResponse(modelo(), total,
                "precoBase=" + r.precoBase() + " × porcoes=" + r.qtdPorcoes());
    }
}
