package br.com.seuprojeto.pascoa.notificacao.repository;

import br.com.seuprojeto.pascoa.notificacao.entity.CanalNotificacao;
import br.com.seuprojeto.pascoa.notificacao.entity.EventoNotificacao;
import br.com.seuprojeto.pascoa.notificacao.entity.NotificacaoEnviada;
import br.com.seuprojeto.pascoa.notificacao.entity.StatusEnvio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificacaoEnviadaRepository extends JpaRepository<NotificacaoEnviada, Long> {

    /**
     * Histórico completo — LEFT JOIN para incluir notificações sem pedido
     * (aniversário, orçamento expirando). Cada associação nullable usa LEFT JOIN FETCH.
     */
    @Query("SELECT DISTINCT n FROM NotificacaoEnviada n " +
           "LEFT JOIN FETCH n.pedido " +
           "LEFT JOIN FETCH n.cliente " +
           "LEFT JOIN FETCH n.orcamento " +
           "ORDER BY n.dataEnvio DESC")
    List<NotificacaoEnviada> findAllComPedido();

    List<NotificacaoEnviada> findByPedidoIdOrderByDataEnvioDesc(Long pedidoId);

    /** Idempotência para notificações de pedido: evita reenvio de mesmo (pedido, evento, canal). */
    boolean existsByPedidoIdAndEventoAndCanalAndStatus(
            Long pedidoId, EventoNotificacao evento, CanalNotificacao canal, StatusEnvio status);

    /** Idempotência para ORCAMENTO_EXPIRANDO: um alerta ENVIADA por (orçamento, evento, canal). */
    boolean existsByOrcamentoIdAndEventoAndCanalAndStatus(
            Long orcamentoId, EventoNotificacao evento, CanalNotificacao canal, StatusEnvio status);

    /**
     * Idempotência para ANIVERSARIO_CLIENTE: verifica se já foi enviado no mesmo ano.
     * Usa SQL nativo para EXTRACT(YEAR FROM data_envio) — compatível com PostgreSQL e H2.
     */
    @Query(value = "SELECT COUNT(*) > 0 FROM notificacoes_enviadas " +
                   "WHERE cliente_id = :clienteId " +
                   "AND evento = :evento " +
                   "AND canal = :canal " +
                   "AND status = 'ENVIADA' " +
                   "AND EXTRACT(YEAR FROM data_envio) = :ano",
           nativeQuery = true)
    boolean jaEnviouAniversarioNoAno(@Param("clienteId") Long clienteId,
                                     @Param("evento") String evento,
                                     @Param("canal") String canal,
                                     @Param("ano") int ano);
}
