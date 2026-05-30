package br.com.seuprojeto.pascoa.notification.usecase;

import br.com.seuprojeto.pascoa.notification.application.port.in.NotificacaoUseCase.EnviarCommand;
import br.com.seuprojeto.pascoa.notification.application.port.out.*;
import br.com.seuprojeto.pascoa.notification.application.usecase.NotificacaoUseCaseImpl;
import br.com.seuprojeto.pascoa.notification.domain.model.Canal;
import br.com.seuprojeto.pascoa.notification.domain.model.Notificacao;
import br.com.seuprojeto.pascoa.notification.domain.model.StatusNotificacao;
import br.com.seuprojeto.pascoa.notification.domain.service.TemplateEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificacaoUseCaseTest {

    @Mock NotificacaoRepositoryPort repository;
    @Mock EmailPort emailPort;
    @Mock WhatsAppPort whatsAppPort;
    @Mock SmsPort smsPort;

    private NotificacaoUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new NotificacaoUseCaseImpl(repository, emailPort, whatsAppPort, smsPort);
    }

    @Test
    @DisplayName("Enviar email com sucesso marca como ENVIADA")
    void enviarEmailSucesso() {
        when(repository.save(any())).thenAnswer(inv -> {
            Notificacao n = inv.getArgument(0);
            return n.withId(1L);
        });
        doNothing().when(emailPort).enviar(any(), any(), any());

        Notificacao enviada = useCase.enviar(new EnviarCommand(
                "ana@example.com", Canal.EMAIL,
                "Pedido confirmado", "Olá {nome}, seu pedido #{orderId} foi confirmado!",
                Map.of("nome", "Ana", "orderId", "42"),
                "order.confirmed", "42"));

        assertThat(enviada.getStatus()).isEqualTo(StatusNotificacao.ENVIADA);
        verify(emailPort).enviar("ana@example.com", "Pedido confirmado",
                "Olá Ana, seu pedido #42 foi confirmado!");
    }

    @Test
    @DisplayName("WhatsApp falha → fallback SMS é tentado")
    void whatsappFallbackSms() {
        when(repository.save(any())).thenAnswer(inv -> {
            Notificacao n = inv.getArgument(0);
            return n.withId(1L);
        });
        doThrow(new UnsupportedOperationException("WhatsApp indisponível"))
                .when(whatsAppPort).enviar(any(), any());
        doNothing().when(smsPort).enviar(any(), any());

        Notificacao enviada = useCase.enviar(new EnviarCommand(
                "11999999999", Canal.WHATSAPP, null, "Pedido pronto!",
                Map.of(), "order.ready", "10"));

        assertThat(enviada.getStatus()).isEqualTo(StatusNotificacao.ENVIADA);
        verify(smsPort).enviar("11999999999", "Pedido pronto!");
    }

    @Test
    @DisplayName("Email falha → status FALHOU com mensagem de erro")
    void emailFalha() {
        when(repository.save(any())).thenAnswer(inv -> {
            Notificacao n = inv.getArgument(0);
            return n.withId(1L);
        });
        doThrow(new RuntimeException("SMTP indisponível"))
                .when(emailPort).enviar(any(), any(), any());

        Notificacao falhou = useCase.enviar(new EnviarCommand(
                "ana@e.com", Canal.EMAIL, "Assunto", "Conteúdo",
                Map.of(), "test", "1"));

        assertThat(falhou.getStatus()).isEqualTo(StatusNotificacao.FALHOU);
        assertThat(falhou.getErroMensagem()).contains("SMTP");
    }

    @Test
    @DisplayName("TemplateEngine substitui variáveis corretamente")
    void templateEngine() {
        String result = TemplateEngine.render(
                "Olá {nome}, pedido #{id} está {status}.",
                Map.of("nome", "Ana", "id", "99", "status", "PRONTO"));

        assertThat(result).isEqualTo("Olá Ana, pedido #99 está PRONTO.");
    }

    @Test
    @DisplayName("TemplateEngine mantém variável se ausente no mapa")
    void templateEngineVarAusente() {
        String result = TemplateEngine.render("Olá {nome}!", Map.of());
        assertThat(result).isEqualTo("Olá {nome}!");
    }

    @Test
    @DisplayName("marcarEnviada e marcarFalhou atualizam estado corretamente")
    void estadoNotificacao() {
        Notificacao n = Notificacao.builder()
                .id(1L).destinatario("test@e.com").canal(Canal.EMAIL)
                .conteudo("msg").status(StatusNotificacao.PENDENTE)
                .criadoEm(LocalDateTime.now()).build();

        Notificacao enviada = n.marcarEnviada();
        assertThat(enviada.getStatus()).isEqualTo(StatusNotificacao.ENVIADA);
        assertThat(enviada.getEnviadoEm()).isNotNull();

        Notificacao falhou = n.marcarFalhou("erro SMTP");
        assertThat(falhou.getStatus()).isEqualTo(StatusNotificacao.FALHOU);
        assertThat(falhou.getErroMensagem()).isEqualTo("erro SMTP");
    }
}
