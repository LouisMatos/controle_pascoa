package br.com.seuprojeto.pascoa.inventory.application.port.in;

import br.com.seuprojeto.pascoa.inventory.domain.model.MateriaPrima;
import br.com.seuprojeto.pascoa.inventory.domain.model.Movimentacao;
import br.com.seuprojeto.pascoa.inventory.domain.model.Unidade;

import java.math.BigDecimal;
import java.util.List;

public interface EstoqueUseCase {

    record CriarMateriaPrimaCommand(
            String nome,
            Unidade unidade,
            BigDecimal estoqueInicial,
            BigDecimal estoqueMinimo,
            Long fornecedorId
    ) {}

    record MovimentarEstoqueCommand(
            Long materiaPrimaId,
            BigDecimal quantidade,
            String observacao
    ) {}

    MateriaPrima criar(CriarMateriaPrimaCommand command);

    MateriaPrima buscarPorId(Long id);

    List<MateriaPrima> listar();

    List<MateriaPrima> listarCriticos();

    Movimentacao registrarEntrada(MovimentarEstoqueCommand command);

    Movimentacao registrarSaida(MovimentarEstoqueCommand command);

    List<Movimentacao> listarMovimentacoes(Long materiaPrimaId);

    boolean verificarDisponibilidade(Long materiaPrimaId, BigDecimal quantidade);
}
