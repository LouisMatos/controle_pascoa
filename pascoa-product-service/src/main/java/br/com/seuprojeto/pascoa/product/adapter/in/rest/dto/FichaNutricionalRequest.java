package br.com.seuprojeto.pascoa.product.adapter.in.rest.dto;

import java.math.BigDecimal;

public record FichaNutricionalRequest(
        BigDecimal porcaoG,
        BigDecimal calorias,
        BigDecimal proteinasG,
        BigDecimal carboidratosG,
        BigDecimal gordurasG,
        BigDecimal gordurasSaturadasG,
        BigDecimal fibrasG,
        BigDecimal sodioMg,
        BigDecimal acucaresG,
        Boolean semGluten,
        Boolean semLactose,
        Boolean vegano,
        Boolean vegetariano,
        String alergenos
) {}
