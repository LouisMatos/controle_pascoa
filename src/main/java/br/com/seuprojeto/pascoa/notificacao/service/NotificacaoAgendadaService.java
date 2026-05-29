package br.com.seuprojeto.pascoa.notificacao.service;

import br.com.seuprojeto.pascoa.cadastro.entity.Cliente;
import br.com.seuprojeto.pascoa.cadastro.repository.ClienteRepository;
import br.com.seuprojeto.pascoa.notificacao.entity.EventoNotificacao;
import br.com.seuprojeto.pascoa.orcamento.entity.Orcamento;
import br.com.seuprojeto.pascoa.orcamento.entity.StatusOrcamento;
import br.com.seuprojeto.pascoa.orcamento.repository.OrcamentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Item 25 — Jobs agendados para notificações proativas.
 *
 * <ul>
 *   <li>{@link #notificarAniversariantes()} — todo dia às 08h00,
 *       envia mensagem de aniversário a clientes com opt-in cujo dia de aniversário é hoje.</li>
 *   <li>{@link #notificarOrcamentosExpirando()} — todo dia às 09h00,
 *       alerta clientes sobre orçamentos PENDENTE que vencem em 2 dias.</li>
 * </ul>
 *
 * <p>Ambos os jobs usam {@link SchedulerLock} para evitar execução duplicada
 * em ambientes com múltiplas instâncias.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificacaoAgendadaService {

    /** Antecedência (em dias) com que o alerta de orçamento expirando é enviado. */
    private static final int DIAS_AVISO_ORCAMENTO = 2;

    private final ClienteRepository    clienteRepository;
    private final OrcamentoRepository  orcamentoRepository;
    private final NotificacaoService   notificacaoService;

    // ══════════════════════════════════════════════════════════════════════════
    // Job 1 — Aniversariantes
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Envia notificação de aniversário a clientes cujo mês e dia de
     * nascimento coincidem com a data de hoje.
     *
     * <p>A idempotência (evitar duplicata no mesmo ano) é garantida em
     * {@link NotificacaoService#processarParaCliente} via consulta ao banco.
     */
    @Scheduled(cron = "0 0 8 * * *")
    @SchedulerLock(name = "notif_aniversariantes",
                   lockAtMostFor = "PT30M",
                   lockAtLeastFor = "PT1M")
    @Transactional
    public void notificarAniversariantes() {
        LocalDate hoje = LocalDate.now();
        List<Cliente> aniversariantes =
                clienteRepository.findAniversariantesHoje(hoje.getMonthValue(), hoje.getDayOfMonth());

        log.info("[NOTIF-AGENDA] Aniversariantes em {}/{}: {} cliente(s)",
                hoje.getDayOfMonth(), hoje.getMonthValue(), aniversariantes.size());

        for (Cliente cliente : aniversariantes) {
            try {
                notificacaoService.processarParaCliente(cliente, EventoNotificacao.ANIVERSARIO_CLIENTE);
            } catch (Exception e) {
                log.error("[NOTIF-AGENDA] Erro ao notificar aniversário cliente={}: {}",
                        cliente.getId(), e.getMessage());
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Job 2 — Orçamentos expirando
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Envia alerta para clientes cujo orçamento PENDENTE vence em
     * {@value DIAS_AVISO_ORCAMENTO} dias.
     *
     * <p>A idempotência (um alerta ENVIADA por orçamento × canal) é garantida
     * por índice único no banco ({@code uq_notif_orcamento_expirando}).
     */
    @Scheduled(cron = "0 0 9 * * *")
    @SchedulerLock(name = "notif_orcamentos_expirando",
                   lockAtMostFor = "PT30M",
                   lockAtLeastFor = "PT1M")
    @Transactional
    public void notificarOrcamentosExpirando() {
        LocalDate dataAlerta = LocalDate.now().plusDays(DIAS_AVISO_ORCAMENTO);
        List<Orcamento> expirando =
                orcamentoRepository.findPendentesComValidadeEm(StatusOrcamento.PENDENTE, dataAlerta);

        log.info("[NOTIF-AGENDA] Orçamentos expirando em {}: {} encontrado(s)",
                dataAlerta, expirando.size());

        for (Orcamento orcamento : expirando) {
            try {
                notificacaoService.processarParaOrcamento(orcamento, EventoNotificacao.ORCAMENTO_EXPIRANDO);
            } catch (Exception e) {
                log.error("[NOTIF-AGENDA] Erro ao notificar orçamento expirando id={}: {}",
                        orcamento.getId(), e.getMessage());
            }
        }
    }
}
