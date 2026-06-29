package br.com.seuprojeto.pascoa.subscription.service;

import br.com.seuprojeto.pascoa.subscription.domain.StatusAssinatura;
import br.com.seuprojeto.pascoa.subscription.repository.AssinaturaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Roda diariamente às 03:00 procurando trials expirando em até 7 dias.
 * Por enquanto apenas loga — quando notification-service estiver integrado via HTTP,
 * dispara email "seu trial expira em X dias".
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TrialExpirationJob {

    private final AssinaturaRepository repo;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional(readOnly = true)
    public void verificarTrialsExpirando() {
        LocalDate hoje = LocalDate.now();
        var expirando = repo.findByStatusAndTrialTerminaEmBetween(
                StatusAssinatura.TRIALING, hoje, hoje.plusDays(7));
        log.info("Trials expirando em até 7 dias: {}", expirando.size());
        for (var a : expirando) {
            long diasRestantes = java.time.temporal.ChronoUnit.DAYS.between(hoje, a.getTrialTerminaEm());
            log.info("  tenant={} dias={} trialTerminaEm={}",
                    a.getTenantId(), diasRestantes, a.getTrialTerminaEm());
            // TODO etapa 14: chamar notification-service /templates/render + envio
        }
    }
}
