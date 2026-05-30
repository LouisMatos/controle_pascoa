package br.com.seuprojeto.pascoa.notification.adapter.out.persistence;

import br.com.seuprojeto.pascoa.notification.domain.model.Canal;
import br.com.seuprojeto.pascoa.notification.domain.model.StatusNotificacao;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificacoes", indexes = {
        @Index(name = "idx_notif_referencia", columnList = "referencia_id"),
        @Index(name = "idx_notif_status",     columnList = "status"),
        @Index(name = "idx_notif_criado_em",  columnList = "criado_em")
})
@Getter
@Setter
public class NotificacaoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String destinatario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Canal canal;

    @Column(length = 300)
    private String assunto;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String conteudo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatusNotificacao status;

    @Column(length = 50)
    private String evento;

    @Column(name = "referencia_id", length = 50)
    private String referenciaId;

    @Column(name = "erro_mensagem", length = 500)
    private String erroMensagem;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "enviado_em")
    private LocalDateTime enviadoEm;
}
