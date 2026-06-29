package br.com.seuprojeto.pascoa.product.adapter.in.rest.dto;

import java.math.BigDecimal;
import java.util.Map;

public record VarianteResponse(
        Long id,
        Long produtoId,
        String sku,
        String nome,
        BigDecimal preco,
        Map<String, String> atributos,
        boolean disponivel
) {}
