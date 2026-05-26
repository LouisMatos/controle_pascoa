package br.com.seuprojeto.pascoa.analytics.dto;

import java.math.BigDecimal;
import java.util.List;

public record SafraDto(int ano, BigDecimal totalFaturamento, long totalPedidos, List<MesDto> meses) {}
