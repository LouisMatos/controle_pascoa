package br.com.seuprojeto.pascoa.financial.adapter.out.persistence;

import br.com.seuprojeto.pascoa.financial.domain.model.CategoriaLancamento;
import br.com.seuprojeto.pascoa.financial.domain.model.TipoLancamento;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "lancamentos", indexes = {
        @Index(name = "idx_lanc_data",    columnList = "data"),
        @Index(name = "idx_lanc_tipo",    columnList = "tipo"),
        @Index(name = "idx_lanc_ref",     columnList = "referencia_id, origem")
})
@Getter
@Setter
public class LancamentoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoLancamento tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoriaLancamento categoria;

    @Column(nullable = false, length = 300)
    private String descricao;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private LocalDate data;

    @Column(name = "referencia_id", length = 50)
    private String referenciaId;

    @Column(length = 50)
    private String origem;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;
}
