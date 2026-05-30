package br.com.seuprojeto.pascoa.inventory.application.port.out;

import br.com.seuprojeto.pascoa.inventory.domain.model.MateriaPrima;

public interface EstoqueEventPublisherPort {
    void publishEstoqueCritico(MateriaPrima materiaPrima);
    void publishSaidaRealizada(Long materiaPrimaId, java.math.BigDecimal quantidade, Long ordemProducaoId);
}
