package br.com.seuprojeto.pascoa.financeiro.entity;

import br.com.seuprojeto.pascoa.cadastro.entity.Fornecedor;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "contas_pagar")
@Data
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContaPagar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Opcional — pode ser uma despesa sem fornecedor específico
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fornecedor_id")
    @ToString.Exclude
    private Fornecedor fornecedor;

    @NotBlank(message = "Descrição é obrigatória")
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String descricao;

    @NotNull(message = "Valor é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @NotNull(message = "Vencimento é obrigatório")
    @Column(nullable = false)
    private LocalDate vencimento;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private StatusConta status = StatusConta.ABERTA;

    @NotNull(message = "Categoria é obrigatória")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoriaContaPagar categoria;

    @Transient
    public boolean isVencida() {
        return status == StatusConta.ABERTA && LocalDate.now().isAfter(vencimento);
    }
}
