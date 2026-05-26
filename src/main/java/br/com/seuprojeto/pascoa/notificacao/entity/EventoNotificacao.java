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
    ORCAMENTO_RECUSADO
}
