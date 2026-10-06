package br.com.seuprojeto.pascoa.analytics.dto;

import java.math.BigDecimal;

public record RankingProdutoDto(String nome, String categoria, BigDecimal quantidade, BigDecimal faturamento) {}
