package br.com.seuprojeto.pascoa.product.adapter.out.persistence;

import br.com.seuprojeto.pascoa.product.domain.model.Categoria;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "produtos")
@Getter
@Setter
public class ProdutoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Categoria categoria;

    @Column(name = "foto_url", length = 500)
    private String fotoUrl;

    @Column(nullable = false)
    private boolean disponivel = true;

    @Column(nullable = false)
    private boolean ativo = true;

    // ── v6 Etapa 8: multi-tenant + sazonalidade + categoria customizada ──
    /** Slug do tenant (X-Tenant-Id). Nullable durante a transição v5→v6. */
    @Column(name = "tenant_id", length = 50)
    private String tenantId;

    /** Categoria livre alinhada ao config-engine; coexiste com enum {@link Categoria} legado. */
    @Column(name = "categoria_customizada", length = 80)
    private String categoriaCustomizada;

    @Column(nullable = false)
    private boolean sazonal = false;

    @Column(name = "sazonal_inicio")
    private LocalDate sazonalInicio;

    @Column(name = "sazonal_fim")
    private LocalDate sazonalFim;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;
}
