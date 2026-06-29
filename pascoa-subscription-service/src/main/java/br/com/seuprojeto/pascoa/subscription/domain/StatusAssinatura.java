package br.com.seuprojeto.pascoa.subscription.domain;

public enum StatusAssinatura {
    TRIALING,           // dentro do período de teste
    ACTIVE,             // pagamento em dia
    PAST_DUE,           // atraso curto, tentando cobrar
    CANCELED,           // cancelada pelo tenant ou pela plataforma
    INCOMPLETE_EXPIRED  // primeiro pagamento falhou, downgrade automático
}
