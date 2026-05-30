package br.com.seuprojeto.pascoa.production.domain.model;

import br.com.seuprojeto.pascoa.production.domain.exception.TransicaoOrdemInvalidaException;
import lombok.Builder;
import lombok.Getter;
import lombok.With;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@With
public class OrdemProducao {

    private final Long id;
    private final Long pedidoId;
    private final String nomeCliente;
    private final StatusOrdem status;
    private final List<ItemOrdem> itens;
    private final LocalDate dataPrevisao;
    private final String observacoes;
    private final LocalDateTime criadoEm;
    private final LocalDateTime atualizadoEm;

    public static OrdemProducao dePedido(Long pedidoId, String nomeCliente,
                                         List<ItemOrdem> itens, LocalDate dataPrevisao) {
        return OrdemProducao.builder()
                .pedidoId(pedidoId)
                .nomeCliente(nomeCliente)
                .status(StatusOrdem.PENDENTE)
                .itens(itens)
                .dataPrevisao(dataPrevisao)
                .criadoEm(LocalDateTime.now())
                .atualizadoEm(LocalDateTime.now())
                .build();
    }

    public OrdemProducao transicionarPara(StatusOrdem novoStatus) {
        if (!status.podeTransicionarPara(novoStatus)) {
            throw new TransicaoOrdemInvalidaException(id, status, novoStatus);
        }
        return this.withStatus(novoStatus).withAtualizadoEm(LocalDateTime.now());
    }

    public boolean estaFinalizada() {
        return status == StatusOrdem.CONCLUIDA || status == StatusOrdem.CANCELADA;
    }
}
