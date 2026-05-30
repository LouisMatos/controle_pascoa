package br.com.seuprojeto.pascoa.notification.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.With;

import java.time.LocalDateTime;

@Getter
@Builder
@With
public class Notificacao {

    private final Long id;
    private final String destinatario;     // email ou telefone
    private final Canal canal;
    private final String assunto;          // usado em EMAIL
    private final String conteudo;
    private final StatusNotificacao status;
    private final String evento;           // ex: order.confirmed
    private final String referenciaId;    // pedidoId, clienteId, etc.
    private final String erroMensagem;
    private final LocalDateTime criadoEm;
    private final LocalDateTime enviadoEm;

    public Notificacao marcarEnviada() {
        return this.withStatus(StatusNotificacao.ENVIADA)
                   .withEnviadoEm(LocalDateTime.now());
    }

    public Notificacao marcarFalhou(String erro) {
        return this.withStatus(StatusNotificacao.FALHOU)
                   .withErroMensagem(erro);
    }
}
