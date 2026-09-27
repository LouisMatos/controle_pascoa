package br.com.seuprojeto.pascoa.notificacao.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "templates_notificacao")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateNotificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "evento_gatilho", nullable = false, length = 30)
    private EventoNotificacao eventoGatilho;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal", nullable = false, length = 10)
    private CanalNotificacao canal;

    /**
     * Assunto do e-mail (ignorado para WhatsApp).
     */
    @Column(name = "assunto", length = 200)
    private String assunto;

    /**
     * Corpo da mensagem com placeholders: {nome}, {numeroPedido},
     * {dataEntrega}, {link}, {valor}.
     */
    @Column(name = "corpo", nullable = false, columnDefinition = "TEXT")
    private String corpo;

    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    /**
     * Descrição das variáveis disponíveis para referência (ex: "{nome}, {dataEntrega}").
     */
    @Column(name = "variaveis", length = 500)
    private String variaveis;
}
