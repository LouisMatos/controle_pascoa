package br.com.seuprojeto.pascoa.seguranca.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Token de redefinição de senha.
 * <p>
 * Ciclo de vida: gerado ao solicitar reset → enviado por e-mail →
 * validado na tela de reset → marcado como {@code usado = true} após uso.
 * Expira automaticamente em 30 minutos ({@code expiraEm}).
 */
@Entity
@Table(name = "password_reset_token")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @ToString.Exclude
    private Usuario usuario;

    /** UUID v4 — gerado em {@link br.com.seuprojeto.pascoa.seguranca.service.PasswordResetService}. */
    @Column(nullable = false, unique = true, length = 36)
    private String token;

    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;

    @Builder.Default
    @Column(nullable = false)
    private Boolean usado = false;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    private void prePersist() {
        if (this.criadoEm == null) this.criadoEm = LocalDateTime.now();
        if (this.usado   == null) this.usado    = false;
    }

    /** Retorna {@code true} se o token ainda não foi usado e não expirou. */
    public boolean isValido() {
        return !Boolean.TRUE.equals(this.usado)
            && LocalDateTime.now().isBefore(this.expiraEm);
    }
}
