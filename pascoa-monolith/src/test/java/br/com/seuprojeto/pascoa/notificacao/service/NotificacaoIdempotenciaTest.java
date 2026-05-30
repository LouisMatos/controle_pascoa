package br.com.seuprojeto.pascoa.notificacao.service;

import br.com.seuprojeto.pascoa.cadastro.entity.Cliente;
import br.com.seuprojeto.pascoa.cadastro.entity.PreferenciaCanal;
import br.com.seuprojeto.pascoa.cadastro.repository.ClienteRepository;
import br.com.seuprojeto.pascoa.notificacao.entity.*;
import br.com.seuprojeto.pascoa.notificacao.repository.NotificacaoEnviadaRepository;
import br.com.seuprojeto.pascoa.pedido.entity.Pedido;
import br.com.seuprojeto.pascoa.pedido.repository.PedidoRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes de integração para a idempotência de notificações (B7).
 *
 * <p>Verifica que:
 * <ul>
 *   <li>O campo {@code evento} é persistido corretamente em {@link NotificacaoEnviada}</li>
 *   <li>{@code existsByPedidoIdAndEventoAndCanalAndStatus} detecta duplicatas</li>
 *   <li>Registros com status FALHA não bloqueiam nova tentativa (idempotência só para ENVIADA)</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificacaoIdempotenciaTest {

    @Autowired private NotificacaoEnviadaRepository enviadaRepository;
    @Autowired private ClienteRepository            clienteRepository;
    @Autowired private PedidoRepository             pedidoRepository;
    @Autowired private EntityManager                em;

    private Pedido pedido;

    @BeforeEach
    void setUp() {
        Cliente cliente = clienteRepository.save(Cliente.builder()
                .nome("Cliente Notif")
                .optIn(true)
                .preferenciaCanal(PreferenciaCanal.EMAIL)
                .build());

        pedido = pedidoRepository.save(Pedido.builder()
                .cliente(cliente)
                .totalPedido(new BigDecimal("100.00"))
                .build());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Campo evento
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Campo 'evento' é persistido e recuperado corretamente")
    void evento_persistidoCorretamente() {
        NotificacaoEnviada enviada = enviadaRepository.save(NotificacaoEnviada.builder()
                .pedido(pedido)
                .evento(EventoNotificacao.PEDIDO_CONFIRMADO)
                .canal(CanalNotificacao.EMAIL)
                .destinatario("teste@pascoa.local")
                .status(StatusEnvio.ENVIADA)
                .build());
        em.flush(); em.clear();

        NotificacaoEnviada recuperada = enviadaRepository.findById(enviada.getId()).orElseThrow();
        assertThat(recuperada.getEvento()).isEqualTo(EventoNotificacao.PEDIDO_CONFIRMADO);
        assertThat(recuperada.getCanal()).isEqualTo(CanalNotificacao.EMAIL);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // existsByPedidoIdAndEventoAndCanalAndStatus
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("existsBy... retorna true quando já existe ENVIADA para (pedido, evento, canal)")
    void existsBy_encontraDuplicata() {
        enviadaRepository.save(NotificacaoEnviada.builder()
                .pedido(pedido)
                .evento(EventoNotificacao.PEDIDO_CONFIRMADO)
                .canal(CanalNotificacao.EMAIL)
                .destinatario("a@b.com")
                .status(StatusEnvio.ENVIADA)
                .build());
        em.flush(); em.clear();

        boolean existe = enviadaRepository.existsByPedidoIdAndEventoAndCanalAndStatus(
                pedido.getId(), EventoNotificacao.PEDIDO_CONFIRMADO,
                CanalNotificacao.EMAIL, StatusEnvio.ENVIADA);

        assertThat(existe).isTrue();
    }

    @Test
    @DisplayName("existsBy... retorna false para evento diferente (mesmo pedido e canal)")
    void existsBy_eventoDiferente_retornaFalse() {
        enviadaRepository.save(NotificacaoEnviada.builder()
                .pedido(pedido)
                .evento(EventoNotificacao.PEDIDO_CONFIRMADO)
                .canal(CanalNotificacao.EMAIL)
                .destinatario("a@b.com")
                .status(StatusEnvio.ENVIADA)
                .build());
        em.flush(); em.clear();

        boolean existe = enviadaRepository.existsByPedidoIdAndEventoAndCanalAndStatus(
                pedido.getId(), EventoNotificacao.PEDIDO_CANCELADO,  // evento diferente
                CanalNotificacao.EMAIL, StatusEnvio.ENVIADA);

        assertThat(existe).isFalse();
    }

    @Test
    @DisplayName("existsBy... retorna false quando só existe FALHA (não bloqueia retry)")
    void existsBy_apenasComFalha_retornaFalse() {
        // Primeira tentativa falhou
        enviadaRepository.save(NotificacaoEnviada.builder()
                .pedido(pedido)
                .evento(EventoNotificacao.PEDIDO_PRONTO)
                .canal(CanalNotificacao.EMAIL)
                .destinatario("c@d.com")
                .status(StatusEnvio.FALHA)
                .mensagemErro("SMTP timeout")
                .build());
        em.flush(); em.clear();

        // Guarda de idempotência: FALHA NÃO bloqueia nova tentativa
        boolean existe = enviadaRepository.existsByPedidoIdAndEventoAndCanalAndStatus(
                pedido.getId(), EventoNotificacao.PEDIDO_PRONTO,
                CanalNotificacao.EMAIL, StatusEnvio.ENVIADA);

        assertThat(existe).isFalse();
    }

    @Test
    @DisplayName("Dois eventos distintos para o mesmo pedido são armazenados independentemente")
    void doisEventos_persistidosIndependentemente() {
        enviadaRepository.save(NotificacaoEnviada.builder()
                .pedido(pedido)
                .evento(EventoNotificacao.PEDIDO_CONFIRMADO)
                .canal(CanalNotificacao.EMAIL)
                .destinatario("a@b.com")
                .status(StatusEnvio.ENVIADA)
                .build());
        enviadaRepository.save(NotificacaoEnviada.builder()
                .pedido(pedido)
                .evento(EventoNotificacao.PEDIDO_PRONTO)
                .canal(CanalNotificacao.EMAIL)
                .destinatario("a@b.com")
                .status(StatusEnvio.ENVIADA)
                .build());
        em.flush(); em.clear();

        List<NotificacaoEnviada> todas = enviadaRepository.findByPedidoIdOrderByDataEnvioDesc(pedido.getId());
        assertThat(todas).hasSize(2);
        assertThat(todas.stream().map(NotificacaoEnviada::getEvento))
                .containsExactlyInAnyOrder(
                        EventoNotificacao.PEDIDO_CONFIRMADO,
                        EventoNotificacao.PEDIDO_PRONTO);
    }

    @Test
    @DisplayName("Canais distintos para mesmo evento e pedido são armazenados independentemente")
    void canaisDistintos_persistidosIndependentemente() {
        enviadaRepository.save(NotificacaoEnviada.builder()
                .pedido(pedido)
                .evento(EventoNotificacao.PEDIDO_ENTREGUE)
                .canal(CanalNotificacao.EMAIL)
                .destinatario("email@b.com")
                .status(StatusEnvio.ENVIADA)
                .build());
        enviadaRepository.save(NotificacaoEnviada.builder()
                .pedido(pedido)
                .evento(EventoNotificacao.PEDIDO_ENTREGUE)
                .canal(CanalNotificacao.WHATSAPP)
                .destinatario("11999990000")
                .status(StatusEnvio.ENVIADA)
                .build());
        em.flush(); em.clear();

        // EMAIL e WHATSAPP são canais distintos — ambos devem existir
        boolean existeEmail = enviadaRepository.existsByPedidoIdAndEventoAndCanalAndStatus(
                pedido.getId(), EventoNotificacao.PEDIDO_ENTREGUE, CanalNotificacao.EMAIL, StatusEnvio.ENVIADA);
        boolean existeWhatsApp = enviadaRepository.existsByPedidoIdAndEventoAndCanalAndStatus(
                pedido.getId(), EventoNotificacao.PEDIDO_ENTREGUE, CanalNotificacao.WHATSAPP, StatusEnvio.ENVIADA);

        assertThat(existeEmail).isTrue();
        assertThat(existeWhatsApp).isTrue();
    }
}
