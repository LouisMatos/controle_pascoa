package br.com.seuprojeto.pascoa.financeiro.entity;

import br.com.seuprojeto.pascoa.pedido.entity.Pedido;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "contas_receber")
@Data
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContaReceber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Pedido é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    @ToString.Exclude
    private Pedido pedido;

    @NotNull(message = "Valor original é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
    @Column(name = "valor_original", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorOriginal;

    @Column(name = "valor_pago", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal valorPago = BigDecimal.ZERO;

    @NotNull(message = "Vencimento é obrigatório")
    @Column(nullable = false)
    private LocalDate vencimento;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private StatusConta status = StatusConta.ABERTA;

    @Transient
    public BigDecimal getSaldo() {
        if (valorPago == null) return valorOriginal;
        return valorOriginal.subtract(valorPago);
    }

    @Transient
    public boolean isVencida() {
        return status == StatusConta.ABERTA && LocalDate.now().isAfter(vencimento);
    }
}
