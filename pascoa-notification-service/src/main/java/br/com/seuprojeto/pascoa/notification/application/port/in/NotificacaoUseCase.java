package br.com.seuprojeto.pascoa.notification.application.port.in;

import br.com.seuprojeto.pascoa.notification.domain.model.Canal;
import br.com.seuprojeto.pascoa.notification.domain.model.Notificacao;

import java.util.List;
import java.util.Map;

public interface NotificacaoUseCase {

    record EnviarCommand(
            String destinatario,
            Canal canal,
            String assunto,
            String templateConteudo,
            Map<String, String> variaveis,
            String evento,
            String referenciaId
    ) {}

    Notificacao enviar(EnviarCommand command);

    Notificacao reenviar(Long id);

    Notificacao buscarPorId(Long id);

    List<Notificacao> listarPorReferencia(String referenciaId);

    List<Notificacao> listarRecentes(int limite);
}
