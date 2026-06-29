package br.com.seuprojeto.pascoa.subscription.service;

import br.com.seuprojeto.pascoa.subscription.domain.*;
import br.com.seuprojeto.pascoa.subscription.exception.SubscriptionException;
import br.com.seuprojeto.pascoa.subscription.repository.AssinaturaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SubscriptionService {

    private static final int TRIAL_DIAS = 14;

    private final AssinaturaRepository repo;
    private final StripeGateway stripe;

    /** Cria assinatura TRIAL + customer Stripe. Idempotente — segunda chamada para o mesmo tenant lança. */
    public Assinatura iniciarTrial(String tenantId, String email) {
        if (repo.existsById(tenantId)) {
            throw new SubscriptionException("Tenant já possui assinatura: " + tenantId);
        }
        String customerId = stripe.criarCustomer(tenantId, email);
        Assinatura a = Assinatura.builder()
                .tenantId(tenantId)
                .plano(Plano.TRIAL)
                .status(StatusAssinatura.TRIALING)
                .stripeCustomerId(customerId)
                .trialTerminaEm(LocalDate.now().plusDays(TRIAL_DIAS))
                .build();
        return repo.save(a);
    }

    /** Move o tenant para um plano pago, criando subscription no Stripe (ou mock). */
    public Assinatura upgrade(String tenantId, Plano novoPlano) {
        if (novoPlano == Plano.TRIAL) {
            throw new SubscriptionException("Não é possível dar upgrade para TRIAL");
        }
        Assinatura a = obrigatoria(tenantId);
        String priceId = stripe.priceIdDoPlano(novoPlano);
        if (stripe.isEnabled() && (priceId == null || priceId.isBlank())) {
            throw new SubscriptionException("stripe.price." + novoPlano.name().toLowerCase() + " não configurado");
        }
        String subId = stripe.criarSubscription(a.getStripeCustomerId(), priceId);
        a.setPlano(novoPlano);
        a.setStatus(StatusAssinatura.ACTIVE);
        a.setStripeSubscriptionId(subId);
        a.setStripePriceId(priceId);
        a.setTrialTerminaEm(null);
        a.setPeriodoFim(LocalDate.now().plusMonths(1));
        log.info("Tenant {} upgrade → {} (sub={})", tenantId, novoPlano, subId);
        return a;
    }

    public Assinatura cancelar(String tenantId) {
        Assinatura a = obrigatoria(tenantId);
        if (a.getStripeSubscriptionId() != null) {
            stripe.cancelarSubscription(a.getStripeSubscriptionId());
        }
        a.setStatus(StatusAssinatura.CANCELED);
        return a;
    }

    /** Downgrade automático invocado por webhook de inadimplência. */
    public Assinatura marcarInadimplencia(String stripeSubscriptionId) {
        Assinatura a = repo.findByStripeSubscriptionId(stripeSubscriptionId)
                .orElseThrow(() -> new SubscriptionException(
                        "Assinatura Stripe não encontrada: " + stripeSubscriptionId));
        a.setStatus(StatusAssinatura.PAST_DUE);
        return a;
    }

    /** Marca pagamento confirmado (Stripe webhook). */
    public Assinatura marcarPagamentoConfirmado(String stripeSubscriptionId, LocalDate novoPeriodoFim) {
        Assinatura a = repo.findByStripeSubscriptionId(stripeSubscriptionId)
                .orElseThrow(() -> new SubscriptionException(
                        "Assinatura Stripe não encontrada: " + stripeSubscriptionId));
        a.setStatus(StatusAssinatura.ACTIVE);
        if (novoPeriodoFim != null) a.setPeriodoFim(novoPeriodoFim);
        return a;
    }

    @Transactional(readOnly = true)
    public Assinatura buscarPorTenant(String tenantId) {
        return obrigatoria(tenantId);
    }

    @Transactional(readOnly = true)
    public LimitesPlano limitesDoTenant(String tenantId) {
        return LimitesPlano.de(obrigatoria(tenantId).getPlano());
    }

    private Assinatura obrigatoria(String tenantId) {
        return repo.findById(tenantId).orElseThrow(() ->
                new SubscriptionException("Tenant sem assinatura: " + tenantId));
    }
}
