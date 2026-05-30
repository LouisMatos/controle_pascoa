package br.com.seuprojeto.pascoa.financeiro.entity;

import br.com.seuprojeto.pascoa.pedido.entity.Pedido;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "despesas_variaveis")
@Data
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DespesaVariavel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Pedido é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    @ToString.Exclude
    private Pedido pedido;

    @NotBlank(message = "Descrição é obrigatória")
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String descricao;

    @NotNull(message = "Valor é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @NotNull(message = "Categoria é obrigatória")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoriaDespesaVariavel categoria;
}
