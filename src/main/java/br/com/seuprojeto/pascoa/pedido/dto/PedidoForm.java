package br.com.seuprojeto.pascoa.pedido.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class PedidoForm {

    private Long id;

    @NotNull(message = "Cliente é obrigatório")
    private Long clienteId;

    @FutureOrPresent(message = "Data de entrega não pode ser no passado")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataEntrega;

    @Size(max = 500)
    private String observacoes;
}
