package br.com.seuprojeto.pascoa.financeiro.dto;

import java.math.BigDecimal;

public record TopProdutoDto(String nome, BigDecimal quantidadeVendida, BigDecimal faturamento) {}
