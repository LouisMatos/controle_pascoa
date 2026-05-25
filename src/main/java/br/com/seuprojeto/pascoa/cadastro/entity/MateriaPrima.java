package br.com.seuprojeto.pascoa.cadastro.entity;

import br.com.seuprojeto.pascoa.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "materias_primas")
@Data
@EqualsAndHashCode(callSuper = false, of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MateriaPrima extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String nome;

    @NotNull(message = "Unidade é obrigatória")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private Unidade unidade;

    @NotNull
    @DecimalMin(value = "0.0", message = "Quantidade não pode ser negativa")
    @Column(name = "quantidade_atual", nullable = false, precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal quantidadeAtual = BigDecimal.ZERO;

    @NotNull
    @DecimalMin(value = "0.0", message = "Quantidade mínima não pode ser negativa")
    @Column(name = "quantidade_minima", nullable = false, precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal quantidadeMinima = BigDecimal.ZERO;

    @NotNull
    @DecimalMin(value = "0.0", message = "Custo não pode ser negativo")
    @Column(name = "custo_unitario", nullable = false, precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal custoUnitario = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "Custo médio ponderado não pode ser negativo")
    @Column(name = "custo_medio_ponderado", precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal custoMedioPonderado = BigDecimal.ZERO;

    @Column(name = "data_ultima_compra")
    private LocalDate dataUltimaCompra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fornecedor_preferencial_id")
    @ToString.Exclude
    private Fornecedor fornecedorPreferencial;

    @Transient
    public boolean isEstoqueCritico() {
        return quantidadeAtual != null
            && quantidadeMinima != null
            && quantidadeAtual.compareTo(quantidadeMinima) < 0;
    }
}
