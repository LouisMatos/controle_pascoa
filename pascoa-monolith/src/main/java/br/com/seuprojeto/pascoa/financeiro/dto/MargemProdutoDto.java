package br.com.seuprojeto.pascoa.financeiro.dto;

import java.math.BigDecimal;

public record MargemProdutoDto(String nome, BigDecimal precoVenda, BigDecimal custoUnitario, BigDecimal margemPct) {}
