package br.com.seuprojeto.pascoa.orcamento.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class OrcamentoForm {

    @NotNull(message = "Cliente é obrigatório")
    private Long clienteId;

    @NotNull(message = "Validade é obrigatória")
    @Future(message = "Validade deve ser uma data futura")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate validade;

    @Size(max = 1000)
    private String observacoes;

    @Valid
    private List<OrcamentoItemForm> itens = new ArrayList<>();
}
