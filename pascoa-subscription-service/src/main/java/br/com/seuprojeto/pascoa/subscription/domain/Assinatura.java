package br.com.seuprojeto.pascoa.subscription.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "assinatura", schema = "subscription")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Assinatura {

    /** tenantId é a chave natural — uma assinatura por tenant. */
    @Id
    @Column(name = "tenant_id", length = 50)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Plano plano;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusAssinatura status;

    @Column(name = "stripe_customer_id",     length = 80) private String stripeCustomerId;
    @Column(name = "stripe_subscription_id", length = 80) private String stripeSubscriptionId;
    @Column(name = "stripe_price_id",        length = 80) private String stripePriceId;

    @Column(name = "trial_termina_em") private LocalDate trialTerminaEm;
    @Column(name = "periodo_fim")      private LocalDate periodoFim;

    @Column(name = "criada_em",     nullable = false) private LocalDateTime criadaEm;
    @Column(name = "atualizada_em") private LocalDateTime atualizadaEm;

    @PrePersist void onCreate() { if (criadaEm == null) criadaEm = LocalDateTime.now(); }
    @PreUpdate  void onUpdate() { atualizadaEm = LocalDateTime.now(); }
}
