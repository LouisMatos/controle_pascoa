package br.com.seuprojeto.pascoa.notificacao.listener;

import br.com.seuprojeto.pascoa.cadastro.entity.Cliente;
import br.com.seuprojeto.pascoa.cadastro.entity.PreferenciaCanal;
import br.com.seuprojeto.pascoa.notificacao.entity.EventoNotificacao;
import br.com.seuprojeto.pascoa.notificacao.event.PedidoStatusEvent;
import br.com.seuprojeto.pascoa.notificacao.service.NotificacaoService;
import br.com.seuprojeto.pascoa.pedido.entity.Pedido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Testes unitários para {@link NotificacaoEventListener}.
 * Usa Mockito — sem contexto Spring, execução síncrona (sem @Async).
 */
@ExtendWith(MockitoExtension.class)
class NotificacaoEventListenerTest {

    @Mock
    private NotificacaoService notificacaoService;

    @InjectMocks
    private NotificacaoEventListener listener;

    private Pedido pedido;

    @BeforeEach
    void setUp() {
        Cliente cliente = Cliente.builder()
                .id(1L)
                .nome("Cliente Teste")
                .email("teste@example.com")
                .telefone("11999999999")
                .optIn(true)
                .preferenciaCanal(PreferenciaCanal.EMAIL)
                .build();

        pedido = Pedido.builder()
                .id(42L)
                .cliente(cliente)
                .totalPedido(new BigDecimal("150.00"))
                .build();
    }

    // ── testes ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Evento PEDIDO_CONFIRMADO → delega ao NotificacaoService.processar()")
    void onPedidoStatus_pedidoConfirmado_delegaAoServico() {
        PedidoStatusEvent event = new PedidoStatusEvent(pedido, EventoNotificacao.PEDIDO_CONFIRMADO);

        listener.onPedidoStatus(event);

        verify(notificacaoService, times(1))
                .processar(pedido, EventoNotificacao.PEDIDO_CONFIRMADO);
        verifyNoMoreInteractions(notificacaoService);
    }

    @ParameterizedTest
    @EnumSource(EventoNotificacao.class)
    @DisplayName("Qualquer EventoNotificacao → NotificacaoService.processar() é sempre chamado")
    void onPedidoStatus_qualquerEvento_sempreDelega(EventoNotificacao evento) {
        PedidoStatusEvent event = new PedidoStatusEvent(pedido, evento);

        listener.onPedidoStatus(event);

        verify(notificacaoService).processar(eq(pedido), eq(evento));
    }

    @Test
    @DisplayName("Exceção no NotificacaoService → listener absorve o erro, não propaga")
    void onPedidoStatus_serviceLancaExcecao_naoPropagraException() {
        doThrow(new RuntimeException("Falha simulada ao enviar notificação"))
                .when(notificacaoService).processar(any(), any());

        PedidoStatusEvent event = new PedidoStatusEvent(pedido, EventoNotificacao.PEDIDO_PRONTO);

        // Não deve lançar nenhuma exceção para o chamador
        assertThatNoException().isThrownBy(() -> listener.onPedidoStatus(event));

        // O serviço ainda foi chamado (falhou internamente)
        verify(notificacaoService).processar(pedido, EventoNotificacao.PEDIDO_PRONTO);
    }

    @Test
    @DisplayName("Exceção de infraestrutura (ex.: NullPointerException) → também absorvida")
    void onPedidoStatus_excecaoDeInfraestrutura_tambemAbsorvida() {
        doThrow(new NullPointerException("Erro inesperado"))
                .when(notificacaoService).processar(any(), any());

        PedidoStatusEvent event = new PedidoStatusEvent(pedido, EventoNotificacao.PAGAMENTO_RECEBIDO);

        assertThatNoException().isThrownBy(() -> listener.onPedidoStatus(event));
    }

    @Test
    @DisplayName("Pedido com id e evento corretos são passados ao serviço sem alteração")
    void onPedidoStatus_passaExatamentePedidoEEvento() {
        EventoNotificacao evento = EventoNotificacao.PEDIDO_ENTREGUE;
        PedidoStatusEvent event  = new PedidoStatusEvent(pedido, evento);

        listener.onPedidoStatus(event);

        // Verifica que o pedido não foi modificado e o evento correto foi passado
        verify(notificacaoService).processar(pedido, evento);
        assertThatNoException().isThrownBy(() -> listener.onPedidoStatus(event));
    }
}
