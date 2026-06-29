package br.com.seuprojeto.pascoa.subscription.service;

import br.com.seuprojeto.pascoa.subscription.domain.Plano;
import br.com.seuprojeto.pascoa.subscription.exception.SubscriptionException;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Subscription;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.SubscriptionCancelParams;
import com.stripe.param.SubscriptionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Wrapper sobre o Stripe Java SDK.
 *
 * <p>Modo MOCK (default em dev — {@code stripe.enabled=false}): retorna IDs sintéticos
 * sem chamar a API real. Permite rodar o serviço sem chave Stripe configurada.
 *
 * <p>Modo REAL ({@code stripe.enabled=true} + {@code stripe.api-key}): chama o SDK.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StripeGateway {

    @Value("${stripe.enabled:false}")     private boolean enabled;
    @Value("${stripe.api-key:}")          private String apiKey;
    @Value("${stripe.price.starter:}")    private String pricePlanStarter;
    @Value("${stripe.price.pro:}")        private String pricePlanPro;
    @Value("${stripe.price.enterprise:}") private String pricePlanEnterprise;

    @PostConstruct
    void init() {
        if (enabled) {
            if (apiKey == null || apiKey.isBlank()) {
                throw new SubscriptionException("stripe.enabled=true mas stripe.api-key vazio");
            }
            Stripe.apiKey = apiKey;
            log.info("Stripe SDK habilitado");
        } else {
            log.info("Stripe em modo MOCK — IDs sintéticos, nenhuma chamada externa");
        }
    }

    public String priceIdDoPlano(Plano plano) {
        return switch (plano) {
            case TRIAL      -> null;                  // trial não cobra
            case STARTER    -> pricePlanStarter;
            case PRO        -> pricePlanPro;
            case ENTERPRISE -> pricePlanEnterprise;
        };
    }

    public String criarCustomer(String tenantId, String email) {
        if (!enabled) return "cus_mock_" + UUID.randomUUID().toString().substring(0, 8);
        try {
            Customer c = Customer.create(CustomerCreateParams.builder()
                    .setEmail(email)
                    .setName(tenantId)
                    .setMetadata(Map.of("tenantId", tenantId))
                    .build());
            log.info("Stripe customer criado: tenant={} id={}", tenantId, c.getId());
            return c.getId();
        } catch (StripeException e) {
            throw new SubscriptionException("Falha ao criar customer Stripe", e);
        }
    }

    public String criarSubscription(String customerId, String priceId) {
        if (!enabled) return "sub_mock_" + UUID.randomUUID().toString().substring(0, 8);
        try {
            Subscription s = Subscription.create(SubscriptionCreateParams.builder()
                    .setCustomer(customerId)
                    .addItem(SubscriptionCreateParams.Item.builder().setPrice(priceId).build())
                    .build());
            log.info("Stripe subscription criada: customer={} sub={}", customerId, s.getId());
            return s.getId();
        } catch (StripeException e) {
            throw new SubscriptionException("Falha ao criar subscription Stripe", e);
        }
    }

    public void cancelarSubscription(String subscriptionId) {
        if (!enabled) {
            log.info("[MOCK] cancelando subscription {}", subscriptionId);
            return;
        }
        try {
            Subscription s = Subscription.retrieve(subscriptionId);
            s.cancel(SubscriptionCancelParams.builder().build());
            log.info("Stripe subscription cancelada: {}", subscriptionId);
        } catch (StripeException e) {
            throw new SubscriptionException("Falha ao cancelar subscription Stripe", e);
        }
    }

    public boolean isEnabled() { return enabled; }
}
