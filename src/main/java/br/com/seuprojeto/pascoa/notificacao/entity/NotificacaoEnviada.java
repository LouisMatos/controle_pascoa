package br.com.seuprojeto.pascoa.notificacao.entity;

import br.com.seuprojeto.pascoa.pedido.entity.Pedido;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificacoes_enviadas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificacaoEnviada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private TemplateNotificacao template;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal", nullable = false, length = 10)
    private CanalNotificacao canal;

    /**
     * Número de telefone (WhatsApp) ou endereço de e-mail.
     */
    @Column(name = "destinatario", nullable = false, length = 200)
    private String destinatario;

    @Column(name = "data_envio", nullable = false)
    @Builder.Default
    private LocalDateTime dataEnvio = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private StatusEnvio status;

    @Column(name = "mensagem_erro", columnDefinition = "TEXT")
    private String mensagemErro;
}
