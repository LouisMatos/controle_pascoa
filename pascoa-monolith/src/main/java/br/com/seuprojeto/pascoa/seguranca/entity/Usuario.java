package br.com.seuprojeto.pascoa.seguranca.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "usuarios")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(unique = true, nullable = false, length = 60)
    private String login;

    @Column(nullable = false)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Builder.Default
    private boolean ativo = true;

    /** E-mail para recuperação de senha (opcional). */
    @Column(length = 150)
    private String email;

    @Column(name = "totp_secret", length = 64)
    private String totpSecret;

    @Builder.Default
    @Column(name = "totp_ativado", nullable = false)
    private boolean totpAtivado = false;

    @Builder.Default
    @Column(name = "tentativas_totp_falhas", nullable = false)
    private int tentativasTotpFalhas = 0;
}
