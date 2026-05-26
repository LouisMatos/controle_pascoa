package br.com.seuprojeto.pascoa.notificacao.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "alertas_internos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertaInterno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String mensagem;

    /** URL para navegar ao clicar na notificação (opcional). */
    @Column(length = 300)
    private String link;

    /** Classe Bootstrap Icons, ex: "bi-clipboard2-x". */
    @Column(nullable = false, length = 60)
    @Builder.Default
    private String icone = "bi-bell";

    /** Cor Bootstrap sem prefixo, ex: "danger", "success", "warning". */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String cor = "secondary";

    @Column(nullable = false)
    @Builder.Default
    private Boolean lido = false;

    @Column(name = "criado_em", nullable = false)
    @Builder.Default
    private LocalDateTime criadoEm = LocalDateTime.now();
}
