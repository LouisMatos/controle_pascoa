package br.com.seuprojeto.pascoa.notificacao.entity;

public enum EventoNotificacao {
    // ── Pedidos (cliente) ──────────────────────────────────────────────────
    PEDIDO_CONFIRMADO,
    PRODUCAO_INICIADA,
    PEDIDO_PRONTO,
    PEDIDO_ENTREGUE,
    PAGAMENTO_RECEBIDO,
    PEDIDO_CANCELADO,
    // ── Orçamentos (novos) ────────────────────────────────────────────────
    ORCAMENTO_APROVADO,
    ORCAMENTO_RECUSADO,
    // ── Notificações proativas (Item 25) ──────────────────────────────────
    /** Enviado no dia do aniversário do cliente via job @Scheduled às 08h. */
    ANIVERSARIO_CLIENTE,
    /** Enviado 2 dias antes do vencimento de orçamento PENDENTE. */
    ORCAMENTO_EXPIRANDO
}
