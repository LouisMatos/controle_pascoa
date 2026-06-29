package br.com.seuprojeto.pascoa.pricing.service;

import br.com.seuprojeto.pascoa.pricing.domain.ModeloPreco;
import br.com.seuprojeto.pascoa.pricing.domain.PricingRequest;
import br.com.seuprojeto.pascoa.pricing.domain.PricingResponse;
import br.com.seuprojeto.pascoa.pricing.exception.PricingException;
import br.com.seuprojeto.pascoa.pricing.strategy.PricingStrategy;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Despacha o {@link PricingRequest} para a {@link PricingStrategy} correspondente.
 * O Spring injeta todas as strategies via lista — adicionar um novo modelo é só
 * implementar a interface; o engine resolve automaticamente.
 */
@Service
public class PricingEngine {

    private final Map<ModeloPreco, PricingStrategy> strategies;

    public PricingEngine(List<PricingStrategy> strategies) {
        this.strategies = new EnumMap<>(ModeloPreco.class);
        for (var s : strategies) this.strategies.put(s.modelo(), s);
        if (this.strategies.size() != ModeloPreco.values().length) {
            throw new IllegalStateException("Faltam strategies para alguns ModeloPreco. Encontradas: " + this.strategies.keySet());
        }
    }

    public PricingResponse calcular(PricingRequest request) {
        if (request == null || request.modelo() == null)
            throw new PricingException("Request inválido — modelo ausente");
        return strategies.get(request.modelo()).calcular(request);
    }
}
