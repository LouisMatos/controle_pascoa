package br.com.seuprojeto.pascoa.production.application.port.out;

import br.com.seuprojeto.pascoa.production.domain.model.OrdemProducao;

public interface ProducaoEventPublisherPort {
    void publishOrdemIniciada(OrdemProducao ordem);
    void publishOrdemConcluida(OrdemProducao ordem);
    void publishOrdemCancelada(OrdemProducao ordem);
}
