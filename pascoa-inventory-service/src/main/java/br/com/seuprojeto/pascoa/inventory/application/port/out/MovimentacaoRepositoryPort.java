package br.com.seuprojeto.pascoa.inventory.application.port.out;

import br.com.seuprojeto.pascoa.inventory.domain.model.Movimentacao;

import java.util.List;

public interface MovimentacaoRepositoryPort {
    Movimentacao save(Movimentacao movimentacao);
    List<Movimentacao> findByMateriaPrimaId(Long materiaPrimaId);
}
