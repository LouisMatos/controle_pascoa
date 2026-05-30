package br.com.seuprojeto.pascoa.pedido.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "pagamentos")
@Data
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    @ToString.Exclude
    private Pedido pedido;

    @NotNull(message = "Valor é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @NotNull(message = "Tipo de pagamento é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pagamento", nullable = false, length = 20)
    private TipoPagamento tipoPagamento;

    @Column(name = "data_pagamento", nullable = false)
    private LocalDate dataPagamento;

    @Column(length = 300)
    private String observacoes;

    @PrePersist
    private void prePersist() {
        if (this.dataPagamento == null) {
            this.dataPagamento = LocalDate.now();
        }
    }
}
