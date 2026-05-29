package br.com.seuprojeto.pascoa.gastos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "gastos_variaveis")
@Data
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GastoVariavel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String descricao;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "data_lancamento", nullable = false)
    private LocalDate dataLancamento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CategoriaGasto categoria;

    @Column(name = "referencia_mes", nullable = false)
    private Integer referenciaMes;

    @Column(name = "referencia_ano", nullable = false)
    private Integer referenciaAno;

    @Column(length = 500)
    private String observacoes;

    @Column(name = "comprovante_url", length = 500)
    private String comprovanteUrl;

    /**
     * B9/F6: Quando true, este gasto foi desconsiderado do cálculo de custo
     * (ex: pedido cancelado — os gastos vinculados são excluídos da apuração).
     */
    @Builder.Default
    @Column(name = "desconsiderar_no_custo", nullable = false)
    private Boolean desconsiderarNoCusto = false;

    /**
     * F6: FK opcional para o pedido que originou este gasto.
     * Permite marcar desconsiderarNoCusto=true quando o pedido for cancelado.
     */
    @Column(name = "pedido_id")
    private Long pedidoId;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "criado_por", length = 100)
    private String criadoPor;

    @PrePersist
    private void prePersist() {
        if (criadoEm == null) criadoEm = LocalDateTime.now();
        if (dataLancamento != null) {
            if (referenciaMes == null) referenciaMes = dataLancamento.getMonthValue();
            if (referenciaAno == null) referenciaAno = dataLancamento.getYear();
        }
    }
}
