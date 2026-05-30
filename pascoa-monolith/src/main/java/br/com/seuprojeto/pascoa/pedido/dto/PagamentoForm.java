package br.com.seuprojeto.pascoa.pedido.dto;

import br.com.seuprojeto.pascoa.pedido.entity.TipoPagamento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PagamentoForm {

    @NotNull(message = "Valor é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
    private BigDecimal valor;

    @NotNull(message = "Tipo de pagamento é obrigatório")
    private TipoPagamento tipoPagamento;

    private LocalDate dataPagamento;

    @Size(max = 300)
    private String observacoes;
}
