package br.com.seuprojeto.pascoa.cadastro.entity;

import br.com.seuprojeto.pascoa.common.entity.BaseEntity;
import br.com.seuprojeto.pascoa.fichaTecnica.entity.FichaTecnica;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidade Produto com Spring Data Auditing e soft-delete.
 */
@Entity
@Table(name = "produtos")
@SQLDelete(sql = "UPDATE produtos SET excluido_em = NOW() WHERE id = ?")
@SQLRestriction("excluido_em IS NULL")
@Data
@EqualsAndHashCode(callSuper = false, of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Produto extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres")
    @Column(nullable = false, length = 150)
    private String nome;

    @Size(max = 500)
    @Column(length = 500)
    private String descricao;

    @NotNull(message = "Categoria é obrigatória")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Categoria categoria;

    @NotNull(message = "Preço de venda é obrigatório")
    @DecimalMin(value = "0.01", message = "Preço deve ser maior que zero")
    @Column(name = "preco_venda", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoVenda;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @DecimalMin(value = "0.0", message = "Margem não pode ser negativa")
    @Column(name = "margem_desejada", precision = 5, scale = 2)
    private BigDecimal margemDesejada;

    @Size(max = 500)
    @Column(name = "foto", length = 500)
    private String foto;

    @Column(name = "inicio_safra")
    private LocalDate inicioSafra;

    @Column(name = "fim_safra")
    private LocalDate fimSafra;

    /** Timestamp de soft-delete — null enquanto o registro estiver ativo. */
    @Column(name = "excluido_em")
    private LocalDateTime excluidoEm;

    @OneToOne(mappedBy = "produto", cascade = CascadeType.ALL, orphanRemoval = true,
              fetch = FetchType.LAZY)
    @ToString.Exclude
    private FichaTecnica fichaTecnica;
}
