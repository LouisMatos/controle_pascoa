package br.com.seuprojeto.pascoa.fichaTecnica.entity;

import br.com.seuprojeto.pascoa.cadastro.entity.MateriaPrima;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

@Entity
@Table(name = "fichas_tecnicas_itens",
       uniqueConstraints = @UniqueConstraint(columnNames = {"ficha_tecnica_id", "materia_prima_id"}))
@Data
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FichaTecnicaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ficha_tecnica_id", nullable = false)
    @ToString.Exclude
    private FichaTecnica fichaTecnica;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "materia_prima_id", nullable = false)
    private MateriaPrima materiaPrima;

    @NotNull(message = "Quantidade é obrigatória")
    @DecimalMin(value = "0.001", message = "Quantidade deve ser maior que zero")
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantidade;

    @Transient
    public BigDecimal getCustoItem() {
        if (materiaPrima == null || materiaPrima.getCustoUnitario() == null || quantidade == null) {
            return BigDecimal.ZERO;
        }
        return quantidade.multiply(materiaPrima.getCustoUnitario());
    }
}
