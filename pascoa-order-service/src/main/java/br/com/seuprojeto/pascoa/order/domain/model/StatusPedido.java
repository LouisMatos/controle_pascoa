package br.com.seuprojeto.pascoa.order.domain.model;

import java.util.Set;

public enum StatusPedido {
    NOVO, CONFIRMADO, EM_PRODUCAO, PRONTO, ENTREGUE, CANCELADO;

    private static final java.util.Map<StatusPedido, Set<StatusPedido>> TRANSICOES = java.util.Map.of(
            NOVO,         Set.of(CONFIRMADO, CANCELADO),
            CONFIRMADO,   Set.of(EM_PRODUCAO, CANCELADO),
            EM_PRODUCAO,  Set.of(PRONTO),
            PRONTO,       Set.of(ENTREGUE),
            ENTREGUE,     Set.of(),
            CANCELADO,    Set.of()
    );

    public boolean podeTransicionarPara(StatusPedido destino) {
        return TRANSICOES.getOrDefault(this, Set.of()).contains(destino);
    }
}
