package br.com.seuprojeto.pascoa.production.adapter.in.rest.dto;

import br.com.seuprojeto.pascoa.production.adapter.out.persistence.HistoricoFaseJpaEntity;

import java.time.LocalDateTime;

public record HistoricoFaseResponse(
        Long id,
        Long ordemId,
        String faseDe,
        String fasePara,
        Integer ordemDe,
        Integer ordemPara,
        LocalDateTime mudadoEm,
        String mudadoPor,
        String observacao
) {
    public static HistoricoFaseResponse from(HistoricoFaseJpaEntity h) {
        return new HistoricoFaseResponse(h.getId(), h.getOrdemId(), h.getFaseDe(), h.getFasePara(),
                h.getOrdemDe(), h.getOrdemPara(), h.getMudadoEm(), h.getMudadoPor(), h.getObservacao());
    }
}
