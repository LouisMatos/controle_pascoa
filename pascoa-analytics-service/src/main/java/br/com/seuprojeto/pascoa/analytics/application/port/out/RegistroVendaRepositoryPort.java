package br.com.seuprojeto.pascoa.analytics.application.port.out;

import br.com.seuprojeto.pascoa.analytics.domain.model.RegistroVenda;

import java.util.List;

public interface RegistroVendaRepositoryPort {
    RegistroVenda save(RegistroVenda registro);
    List<RegistroVenda> findByAno(int ano);
    boolean existsByPedidoId(Long pedidoId);
}
