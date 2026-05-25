package br.com.seuprojeto.pascoa.pedido.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PedidoForm {

    private Long id;

    @NotNull(message = "Cliente é obrigatório")
    private Long clienteId;

    private LocalDate dataEntrega;

    @Size(max = 500)
    private String observacoes;
}
