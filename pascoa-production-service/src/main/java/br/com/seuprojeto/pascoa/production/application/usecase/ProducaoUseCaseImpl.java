package br.com.seuprojeto.pascoa.production.application.usecase;

import br.com.seuprojeto.pascoa.production.application.port.in.ProducaoUseCase;
import br.com.seuprojeto.pascoa.production.application.port.out.OrdemRepositoryPort;
import br.com.seuprojeto.pascoa.production.application.port.out.ProducaoEventPublisherPort;
import br.com.seuprojeto.pascoa.production.domain.exception.OrdemNotFoundException;
import br.com.seuprojeto.pascoa.production.domain.model.OrdemProducao;
import br.com.seuprojeto.pascoa.production.domain.model.StatusOrdem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ProducaoUseCaseImpl implements ProducaoUseCase {

    private final OrdemRepositoryPort repository;
    private final ProducaoEventPublisherPort eventPublisher;

    @Override
    public OrdemProducao criar(CriarOrdemCommand cmd) {
        OrdemProducao ordem = OrdemProducao.dePedido(
                cmd.pedidoId(), cmd.nomeCliente(), cmd.itens(), cmd.dataPrevisao());
        return repository.save(ordem);
    }

    @Override
    @Transactional(readOnly = true)
    public OrdemProducao buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new OrdemNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public OrdemProducao buscarPorPedidoId(Long pedidoId) {
        return repository.findByPedidoId(pedidoId)
                .orElseThrow(() -> new OrdemNotFoundException(pedidoId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdemProducao> listar() {
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdemProducao> listarPorStatus(StatusOrdem status) {
        return repository.findByStatus(status);
    }

    @Override
    public OrdemProducao iniciar(Long id) {
        OrdemProducao ordem = buscarPorId(id);
        OrdemProducao iniciada = ordem.transicionarPara(StatusOrdem.EM_ANDAMENTO);
        OrdemProducao salva = repository.save(iniciada);
        eventPublisher.publishOrdemIniciada(salva);
        return salva;
    }

    @Override
    public OrdemProducao concluir(Long id) {
        OrdemProducao ordem = buscarPorId(id);
        OrdemProducao concluida = ordem.transicionarPara(StatusOrdem.CONCLUIDA);
        OrdemProducao salva = repository.save(concluida);
        eventPublisher.publishOrdemConcluida(salva);
        return salva;
    }

    @Override
    public OrdemProducao cancelar(Long id) {
        OrdemProducao ordem = buscarPorId(id);
        OrdemProducao cancelada = ordem.transicionarPara(StatusOrdem.CANCELADA);
        OrdemProducao salva = repository.save(cancelada);
        eventPublisher.publishOrdemCancelada(salva);
        return salva;
    }
}
