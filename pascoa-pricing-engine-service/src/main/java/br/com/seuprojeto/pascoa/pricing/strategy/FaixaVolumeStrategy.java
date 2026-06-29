package br.com.seuprojeto.pascoa.pricing.strategy;

import br.com.seuprojeto.pascoa.pricing.domain.ModeloPreco;
import br.com.seuprojeto.pascoa.pricing.domain.PricingRequest;
import br.com.seuprojeto.pascoa.pricing.domain.PricingResponse;
import br.com.seuprojeto.pascoa.pricing.exception.PricingException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Tabela de faixas — ex: 1-11 R$5; 12-99 R$4,50; 100+ R$4.
 * A faixa selecionada é aquela cujo intervalo {@code [minQtd, maxQtd]} contém a quantidade.
 * Use {@code maxQtd = Integer.MAX_VALUE} para "X+" (sem teto).
 */
@Component
public class FaixaVolumeStrategy implements PricingStrategy {
    @Override public ModeloPreco modelo() { return ModeloPreco.FAIXA_VOLUME; }

    @Override
    public PricingResponse calcular(PricingRequest r) {
        if (r.quantidade() == null || r.faixas() == null || r.faixas().isEmpty())
            throw new PricingException("FAIXA_VOLUME requer quantidade e faixas");
        int qtd = r.quantidade().intValueExact();
        var faixa = r.faixas().stream()
                .filter(f -> qtd >= f.minQtd() && qtd <= f.maxQtd())
                .findFirst()
                .orElseThrow(() -> new PricingException(
                        "Nenhuma faixa atende quantidade=" + qtd));
        BigDecimal total = faixa.precoUnitario().multiply(BigDecimal.valueOf(qtd))
                .setScale(2, RoundingMode.HALF_UP);
        return new PricingResponse(modelo(), total,
                "faixa=" + faixa.minQtd() + "-" + faixa.maxQtd()
                        + " precoUnit=" + faixa.precoUnitario() + " × qtd=" + qtd);
    }
}
