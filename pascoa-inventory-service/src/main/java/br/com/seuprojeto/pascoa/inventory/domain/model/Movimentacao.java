package br.com.seuprojeto.pascoa.inventory.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class Movimentacao {

    private final Long id;
    private final Long materiaPrimaId;
    private final TipoMovimentacao tipo;
    private final BigDecimal quantidade;
    private final String observacao;
    private final LocalDateTime dataMovimentacao;
}
