package br.com.seuprojeto.pascoa.estoque.entity;

import br.com.seuprojeto.pascoa.cadastro.entity.MateriaPrima;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimentacoes_estoque")
@Data
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimentacaoEstoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "materia_prima_id", nullable = false)
    private MateriaPrima materiaPrima;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoMovimentacao tipo;

    // Para ENTRADA/SAIDA: sempre positivo. Para AJUSTE: pode ser negativo (redução) ou positivo (aumento).
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantidade;

    @Column(name = "saldo_apos", nullable = false, precision = 12, scale = 3)
    private BigDecimal saldoApos;

    @Column(nullable = false)
    private LocalDateTime data;

    @Column(length = 500)
    private String motivo;

    @Column(length = 100)
    private String usuario;

    // Custo unitário registrado na ENTRADA para rastreabilidade do preço
    @Column(name = "custo_unitario", precision = 10, scale = 4)
    private BigDecimal custoUnitario;

    @PrePersist
    private void prePersist() {
        if (this.data == null) this.data = LocalDateTime.now();
        if (this.usuario == null) this.usuario = "Sistema";
    }
}
