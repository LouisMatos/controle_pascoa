package br.com.seuprojeto.pascoa.financial.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.With;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
@With
public class Lancamento {

    private final Long id;
    private final TipoLancamento tipo;
    private final CategoriaLancamento categoria;
    private final String descricao;
    private final BigDecimal valor;
    private final LocalDate data;
    private final String referenciaId;  // pedidoId, ordemId, etc.
    private final String origem;        // manual, order-service, production-service

    public boolean isReceita() {
        return tipo == TipoLancamento.RECEITA;
    }

    public boolean isDespesa() {
        return tipo == TipoLancamento.DESPESA;
    }
}
