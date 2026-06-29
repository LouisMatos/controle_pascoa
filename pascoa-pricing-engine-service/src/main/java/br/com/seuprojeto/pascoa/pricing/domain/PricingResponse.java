package br.com.seuprojeto.pascoa.pricing.domain;

import java.math.BigDecimal;

public record PricingResponse(
        ModeloPreco modelo,
        BigDecimal precoFinal,
        String detalhe
) {}
