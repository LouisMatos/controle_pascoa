package br.com.seuprojeto.pascoa.notificacao.entity;

import br.com.seuprojeto.pascoa.cadastro.entity.Cliente;
import br.com.seuprojeto.pascoa.orcamento.entity.Orcamento;
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

    /**
     * Pedido associado — nullable para notificações sem pedido
     * (ex: ANIVERSARIO_CLIENTE, ORCAMENTO_EXPIRANDO).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = true)
    private Pedido pedido;

    /** Item 25: cliente direto (para notificações sem pedido, ex: aniversário). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = true)
    private Cliente cliente;

    /** Item 25: orçamento associado (para ORCAMENTO_EXPIRANDO). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orcamento_id", nullable = true)
    private Orcamento orcamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private TemplateNotificacao template;

    /** Evento que originou o envio — usado para garantir idempotência (ver índice uq_notif_pedido_evento_canal). */
    @Enumerated(EnumType.STRING)
    @Column(name = "evento", length = 40)
    private EventoNotificacao evento;

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
