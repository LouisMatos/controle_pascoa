package br.com.seuprojeto.pascoa.subscription.service;

import br.com.seuprojeto.pascoa.subscription.domain.*;
import br.com.seuprojeto.pascoa.subscription.exception.SubscriptionException;
import br.com.seuprojeto.pascoa.subscription.repository.UsoMetricoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Metering de uso por tenant + competência (YYYY-MM). Consumido por:
 * <ul>
 *   <li>Webhooks dos demais microsserviços ao processar evento (incrementa
 *       counters via {@link #incrementar})</li>
 *   <li>Endpoint de leitura para painel do tenant + verificação de cota</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MeteringService {

    private final UsoMetricoRepository repo;
    private final SubscriptionService subscriptionService;

    public enum Metrica { PEDIDOS, USUARIOS_ATIVOS, NOTIFICACOES, ARMAZENAMENTO_MB }

    public UsoMetrico incrementar(String tenantId, Metrica metrica, long delta) {
        String competencia = UsoMetrico.competenciaAtual();
        UsoMetrico u = repo.findByTenantIdAndCompetencia(tenantId, competencia)
                .orElseGet(() -> UsoMetrico.builder()
                        .tenantId(tenantId).competencia(competencia).build());
        switch (metrica) {
            case PEDIDOS         -> u.setPedidosTotal(u.getPedidosTotal() + (int) delta);
            case USUARIOS_ATIVOS -> u.setUsuariosAtivos((int) Math.max(u.getUsuariosAtivos(), delta));
            case NOTIFICACOES    -> u.setNotificacoesTotal(u.getNotificacoesTotal() + (int) delta);
            case ARMAZENAMENTO_MB-> u.setArmazenamentoMb(u.getArmazenamentoMb() + delta);
        }
        return repo.save(u);
    }

    @Transactional(readOnly = true)
    public UsoMetrico usoCorrente(String tenantId) {
        return repo.findByTenantIdAndCompetencia(tenantId, UsoMetrico.competenciaAtual())
                .orElseGet(() -> UsoMetrico.builder()
                        .tenantId(tenantId).competencia(UsoMetrico.competenciaAtual()).build());
    }

    @Transactional(readOnly = true)
    public List<UsoMetrico> historico(String tenantId) {
        return repo.findByTenantIdOrderByCompetenciaDesc(tenantId);
    }

    /** Verifica se uma operação está dentro da cota do plano. Lança {@link SubscriptionException} se excedeu. */
    public void verificarCota(String tenantId, Metrica metrica) {
        LimitesPlano limites = subscriptionService.limitesDoTenant(tenantId);
        UsoMetrico u = usoCorrente(tenantId);
        int limite = switch (metrica) {
            case PEDIDOS         -> limites.pedidosPorMes();
            case USUARIOS_ATIVOS -> limites.usuarios();
            case NOTIFICACOES    -> limites.notificacoesPorMes();
            case ARMAZENAMENTO_MB-> (int) Math.min(limites.armazenamentoMb(), Integer.MAX_VALUE);
        };
        int uso = switch (metrica) {
            case PEDIDOS         -> u.getPedidosTotal();
            case USUARIOS_ATIVOS -> u.getUsuariosAtivos();
            case NOTIFICACOES    -> u.getNotificacoesTotal();
            case ARMAZENAMENTO_MB-> (int) Math.min(u.getArmazenamentoMb(), Integer.MAX_VALUE);
        };
        if (uso >= limite) {
            throw new SubscriptionException("Cota " + metrica + " excedida: " + uso + "/" + limite);
        }
    }
}
