package br.com.seuprojeto.pascoa.inventory.application.usecase;

import br.com.seuprojeto.pascoa.inventory.application.port.in.EstoqueUseCase;
import br.com.seuprojeto.pascoa.inventory.application.port.out.EstoqueEventPublisherPort;
import br.com.seuprojeto.pascoa.inventory.application.port.out.MateriaPrimaRepositoryPort;
import br.com.seuprojeto.pascoa.inventory.application.port.out.MovimentacaoRepositoryPort;
import br.com.seuprojeto.pascoa.inventory.domain.exception.MateriaPrimaNotFoundException;
import br.com.seuprojeto.pascoa.inventory.domain.model.MateriaPrima;
import br.com.seuprojeto.pascoa.inventory.domain.model.Movimentacao;
import br.com.seuprojeto.pascoa.inventory.domain.model.TipoMovimentacao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class EstoqueUseCaseImpl implements EstoqueUseCase {

    private final MateriaPrimaRepositoryPort materiaPrimaRepository;
    private final MovimentacaoRepositoryPort movimentacaoRepository;
    private final EstoqueEventPublisherPort eventPublisher;

    @Override
    public MateriaPrima criar(CriarMateriaPrimaCommand cmd) {
        MateriaPrima mp = MateriaPrima.builder()
                .nome(cmd.nome())
                .unidade(cmd.unidade())
                .quantidadeEstoque(cmd.estoqueInicial() != null ? cmd.estoqueInicial() : BigDecimal.ZERO)
                .estoqueMinimo(cmd.estoqueMinimo() != null ? cmd.estoqueMinimo() : BigDecimal.ZERO)
                .fornecedorId(cmd.fornecedorId())
                .ativo(true)
                .build();
        return materiaPrimaRepository.save(mp);
    }

    @Override
    @Transactional(readOnly = true)
    public MateriaPrima buscarPorId(Long id) {
        return materiaPrimaRepository.findById(id)
                .orElseThrow(() -> new MateriaPrimaNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MateriaPrima> listar() {
        return materiaPrimaRepository.findAllAtivos();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MateriaPrima> listarCriticos() {
        return materiaPrimaRepository.findCriticos();
    }

    @Override
    public Movimentacao registrarEntrada(MovimentarEstoqueCommand cmd) {
        MateriaPrima mp = buscarPorId(cmd.materiaPrimaId());
        MateriaPrima atualizado = mp.registrarEntrada(cmd.quantidade());
        materiaPrimaRepository.save(atualizado);

        return movimentacaoRepository.save(Movimentacao.builder()
                .materiaPrimaId(cmd.materiaPrimaId())
                .tipo(TipoMovimentacao.ENTRADA)
                .quantidade(cmd.quantidade())
                .observacao(cmd.observacao())
                .dataMovimentacao(LocalDateTime.now())
                .build());
    }

    @Override
    public Movimentacao registrarSaida(MovimentarEstoqueCommand cmd) {
        MateriaPrima mp = buscarPorId(cmd.materiaPrimaId());
        MateriaPrima atualizado = mp.registrarSaida(cmd.quantidade());
        materiaPrimaRepository.save(atualizado);

        Movimentacao mov = movimentacaoRepository.save(Movimentacao.builder()
                .materiaPrimaId(cmd.materiaPrimaId())
                .tipo(TipoMovimentacao.SAIDA)
                .quantidade(cmd.quantidade())
                .observacao(cmd.observacao())
                .dataMovimentacao(LocalDateTime.now())
                .build());

        if (atualizado.estoqueEstaCritico()) {
            eventPublisher.publishEstoqueCritico(atualizado);
        }
        eventPublisher.publishSaidaRealizada(cmd.materiaPrimaId(), cmd.quantidade(), null);
        return mov;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Movimentacao> listarMovimentacoes(Long materiaPrimaId) {
        buscarPorId(materiaPrimaId);
        return movimentacaoRepository.findByMateriaPrimaId(materiaPrimaId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verificarDisponibilidade(Long materiaPrimaId, BigDecimal quantidade) {
        return materiaPrimaRepository.findById(materiaPrimaId)
                .map(mp -> mp.getQuantidadeEstoque().compareTo(quantidade) >= 0)
                .orElse(false);
    }
}
