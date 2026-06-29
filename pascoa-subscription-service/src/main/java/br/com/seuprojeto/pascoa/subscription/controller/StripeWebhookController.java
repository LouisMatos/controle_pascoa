package br.com.seuprojeto.pascoa.subscription.controller;

import br.com.seuprojeto.pascoa.subscription.service.SubscriptionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.ApiResource;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * Endpoint para receber webhooks Stripe:
 * <ul>
 *   <li>{@code invoice.payment_succeeded} → marca {@code ACTIVE} + estende período</li>
 *   <li>{@code invoice.payment_failed}    → {@code PAST_DUE}</li>
 *   <li>{@code customer.subscription.deleted} → {@code CANCELED}</li>
 * </ul>
 *
 * <p>Modo MOCK (stripe.enabled=false): aceita payload sem verificar assinatura — só dev.
 * Em PROD, valida {@code Stripe-Signature} com {@code stripe.webhook-secret}.
 */
@RestController
@RequestMapping("/webhooks/stripe")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {

    @Value("${stripe.enabled:false}")   private boolean stripeEnabled;
    @Value("${stripe.webhook-secret:}") private String webhookSecret;

    private final SubscriptionService subscriptionService;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<Map<String, String>> receber(@RequestBody String payload,
                                                       @RequestHeader(value = "Stripe-Signature", required = false) String sig) {
        // MS-07 — HMAC obrigatório quando Stripe está habilitado; opcionalmente
        // exigido em MOCK quando o secret foi configurado (defesa em profundidade).
        boolean exigeAssinatura = stripeEnabled
                || (webhookSecret != null && !webhookSecret.isBlank());

        Event event;
        if (exigeAssinatura) {
            if (webhookSecret == null || webhookSecret.isBlank()) {
                log.error("Stripe habilitado sem webhook-secret configurado.");
                return ResponseEntity.internalServerError().body(Map.of("error","webhook-secret ausente"));
            }
            if (sig == null || sig.isBlank()) {
                log.warn("Webhook Stripe recebido sem header Stripe-Signature.");
                return ResponseEntity.badRequest().body(Map.of("error","Stripe-Signature ausente"));
            }
            try {
                event = Webhook.constructEvent(payload, sig, webhookSecret);
            } catch (SignatureVerificationException e) {
                log.warn("Webhook com assinatura inválida: {}", e.getMessage());
                return ResponseEntity.badRequest().body(Map.of("error","signature inválida"));
            }
        } else {
            event = ApiResource.GSON.fromJson(payload, Event.class);
            log.warn("[MOCK] webhook aceito sem verificação de assinatura — type={}. " +
                    "Em produção, defina STRIPE_ENABLED=true e STRIPE_WEBHOOK_SECRET.",
                    event.getType());
        }

        String subscriptionId = extractSubscriptionId(payload);
        switch (event.getType()) {
            case "invoice.payment_succeeded" -> {
                if (subscriptionId != null)
                    subscriptionService.marcarPagamentoConfirmado(subscriptionId, LocalDate.now().plusMonths(1));
            }
            case "invoice.payment_failed" -> {
                if (subscriptionId != null) subscriptionService.marcarInadimplencia(subscriptionId);
            }
            case "customer.subscription.deleted" -> {
                if (subscriptionId != null) {
                    try {
                        var a = subscriptionService.marcarPagamentoConfirmado(subscriptionId, null);
                        subscriptionService.cancelar(a.getTenantId());
                    } catch (Exception ignore) { /* assinatura local não encontrada */ }
                }
            }
            default -> log.info("Stripe event ignorado: {}", event.getType());
        }
        return ResponseEntity.ok(Map.of("received", "true"));
    }

    /**
     * Extrai o subscription id do payload via Jackson — funciona tanto para invoices
     * ({@code data.object.subscription}) quanto para subscription events ({@code data.object.id}).
     */
    private String extractSubscriptionId(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode obj = root.path("data").path("object");
            JsonNode sub = obj.get("subscription");
            if (sub != null && !sub.isNull() && sub.isTextual()) return sub.asText();
            JsonNode id = obj.get("id");
            if (id != null && !id.isNull() && id.isTextual()) return id.asText();
        } catch (Exception e) {
            log.warn("Não foi possível extrair subscription id: {}", e.getMessage());
        }
        return null;
    }
}
