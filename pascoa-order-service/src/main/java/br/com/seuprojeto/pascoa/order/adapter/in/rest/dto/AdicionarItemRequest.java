package br.com.seuprojeto.pascoa.order.adapter.in.rest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AdicionarItemRequest(@NotNull Long produtoId, @Min(1) int quantidade) {}
