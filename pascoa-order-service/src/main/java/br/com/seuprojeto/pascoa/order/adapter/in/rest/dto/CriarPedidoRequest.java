package br.com.seuprojeto.pascoa.order.adapter.in.rest.dto;

import jakarta.validation.constraints.NotNull;

public record CriarPedidoRequest(@NotNull Long clienteId, String observacao) {}
