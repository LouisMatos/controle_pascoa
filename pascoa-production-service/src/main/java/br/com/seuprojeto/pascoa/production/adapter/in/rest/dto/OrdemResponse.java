package br.com.seuprojeto.pascoa.production.adapter.in.rest.dto;

import br.com.seuprojeto.pascoa.production.domain.model.ItemOrdem;
import br.com.seuprojeto.pascoa.production.domain.model.OrdemProducao;
import br.com.seuprojeto.pascoa.production.domain.model.StatusOrdem;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record OrdemResponse(
        Long id,
        Long pedidoId,
        String nomeCliente,
        StatusOrdem status,
        List<ItemOrdem> itens,
        LocalDate dataPrevisao,
        String observacoes,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
    public static OrdemResponse from(OrdemProducao o) {
        return new OrdemResponse(o.getId(), o.getPedidoId(), o.getNomeCliente(),
                o.getStatus(), o.getItens(), o.getDataPrevisao(), o.getObservacoes(),
                o.getCriadoEm(), o.getAtualizadoEm());
    }
}
