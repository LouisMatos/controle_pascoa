package br.com.seuprojeto.pascoa.production.application.port.in;

import br.com.seuprojeto.pascoa.production.domain.model.ItemOrdem;
import br.com.seuprojeto.pascoa.production.domain.model.OrdemProducao;
import br.com.seuprojeto.pascoa.production.domain.model.StatusOrdem;

import java.time.LocalDate;
import java.util.List;

public interface ProducaoUseCase {

    record CriarOrdemCommand(
            Long pedidoId,
            String nomeCliente,
            List<ItemOrdem> itens,
            LocalDate dataPrevisao
    ) {}

    OrdemProducao criar(CriarOrdemCommand command);

    OrdemProducao buscarPorId(Long id);

    OrdemProducao buscarPorPedidoId(Long pedidoId);

    List<OrdemProducao> listar();

    List<OrdemProducao> listarPorStatus(StatusOrdem status);

    OrdemProducao iniciar(Long id);

    OrdemProducao concluir(Long id);

    OrdemProducao cancelar(Long id);
}
