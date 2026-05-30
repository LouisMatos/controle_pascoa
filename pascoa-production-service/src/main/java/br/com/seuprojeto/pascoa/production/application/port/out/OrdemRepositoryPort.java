package br.com.seuprojeto.pascoa.production.application.port.out;

import br.com.seuprojeto.pascoa.production.domain.model.OrdemProducao;
import br.com.seuprojeto.pascoa.production.domain.model.StatusOrdem;

import java.util.List;
import java.util.Optional;

public interface OrdemRepositoryPort {
    Optional<OrdemProducao> findById(Long id);
    Optional<OrdemProducao> findByPedidoId(Long pedidoId);
    List<OrdemProducao> findAll();
    List<OrdemProducao> findByStatus(StatusOrdem status);
    OrdemProducao save(OrdemProducao ordem);
}
