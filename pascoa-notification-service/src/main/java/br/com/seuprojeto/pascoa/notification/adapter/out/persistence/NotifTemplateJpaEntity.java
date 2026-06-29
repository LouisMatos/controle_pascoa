package br.com.seuprojeto.pascoa.notification.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notif_template")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotifTemplateJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** NULL = template global default. */
    @Column(name = "tenant_id", length = 50)
    private String tenantId;

    @Column(nullable = false, length = 50)
    private String evento;

    @Column(nullable = false, length = 10)
    private String canal;

    @Column(length = 300)
    private String assunto;

    @Column(nullable = false, columnDefinition = "text")
    private String conteudo;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @PrePersist void onCreate() { if (criadoEm == null) criadoEm = LocalDateTime.now(); }
    @PreUpdate  void onUpdate() { atualizadoEm = LocalDateTime.now(); }
}
