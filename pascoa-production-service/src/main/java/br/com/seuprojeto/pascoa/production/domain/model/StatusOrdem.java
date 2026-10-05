package br.com.seuprojeto.pascoa.production.domain.model;

import java.util.Map;
import java.util.Set;

public enum StatusOrdem {
    PENDENTE, EM_ANDAMENTO, CONCLUIDA, CANCELADA;

    private static final Map<StatusOrdem, Set<StatusOrdem>> TRANSICOES = Map.of(
            PENDENTE,     Set.of(EM_ANDAMENTO, CANCELADA),
            EM_ANDAMENTO, Set.of(CONCLUIDA, CANCELADA),
            CONCLUIDA,    Set.of(),
            CANCELADA,    Set.of()
    );

    public boolean podeTransicionarPara(StatusOrdem destino) {
        return TRANSICOES.getOrDefault(this, Set.of()).contains(destino);
    }
}
