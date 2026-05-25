package br.com.seuprojeto.pascoa.cadastro.entity;

import br.com.seuprojeto.pascoa.common.entity.BaseEntity;
import br.com.seuprojeto.pascoa.pedido.entity.Pedido;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade Cliente com:
 * - Spring Data Auditing (criadoEm mapeado para a coluna legada "data_cadastro",
 *   atualizadoEm, criadoPor, atualizadoPor herdados de BaseEntity)
 * - Soft-delete: o DELETE físico é substituído por UPDATE SET excluido_em = NOW()
 *   e todas as queries filtram automaticamente "excluido_em IS NULL"
 */
@Entity
@Table(name = "clientes")
// Mapeia o campo herdado criadoEm para a coluna legada "data_cadastro"
@AttributeOverride(name = "criadoEm",
    column = @Column(name = "data_cadastro", nullable = false, updatable = false))
// Soft-delete
@SQLDelete(sql = "UPDATE clientes SET excluido_em = NOW() WHERE id = ?")
@SQLRestriction("excluido_em IS NULL")
@Data
@EqualsAndHashCode(callSuper = false, of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres")
    @Column(nullable = false, length = 150)
    private String nome;

    @Size(max = 20)
    @Column(length = 20)
    private String telefone;

    @Email(message = "E-mail inválido")
    @Size(max = 150)
    @Column(length = 150)
    private String email;

    @Size(max = 300)
    @Column(length = 300)
    private String endereco;

    @Size(max = 14)
    @Column(length = 14)
    private String cpf;

    /** Timestamp de soft-delete — null enquanto o registro estiver ativo. */
    @Column(name = "excluido_em")
    private LocalDateTime excluidoEm;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferencia_canal", length = 10)
    @Builder.Default
    private PreferenciaCanal preferenciaCanal = PreferenciaCanal.NENHUM;

    @Column(name = "opt_in")
    @Builder.Default
    private Boolean optIn = false;

    @OneToMany(mappedBy = "cliente", fetch = FetchType.LAZY)
    @ToString.Exclude
    @Builder.Default
    private List<Pedido> pedidos = new ArrayList<>();

    @PrePersist
    private void prePersist() {
        // criadoEm (= data_cadastro) é preenchido pelo @CreatedDate do Spring Data Auditing
        if (this.preferenciaCanal == null) this.preferenciaCanal = PreferenciaCanal.NENHUM;
        if (this.optIn == null) this.optIn = false;
    }
}
