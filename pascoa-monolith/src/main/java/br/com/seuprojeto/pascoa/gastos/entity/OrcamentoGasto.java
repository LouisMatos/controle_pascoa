package br.com.seuprojeto.pascoa.gastos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "orcamentos_gasto",
       uniqueConstraints = @UniqueConstraint(columnNames = {"categoria", "referencia_mes", "referencia_ano"}))
@Data
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrcamentoGasto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CategoriaGasto categoria;

    @Column(name = "valor_orcado", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorOrcado;

    @Column(name = "referencia_mes", nullable = false)
    private Integer referenciaMes;

    @Column(name = "referencia_ano", nullable = false)
    private Integer referenciaAno;
}
