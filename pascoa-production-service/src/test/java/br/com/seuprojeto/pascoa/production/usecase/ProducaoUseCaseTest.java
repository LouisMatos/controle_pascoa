package br.com.seuprojeto.pascoa.production.usecase;

import br.com.seuprojeto.pascoa.production.application.port.in.ProducaoUseCase.CriarOrdemCommand;
import br.com.seuprojeto.pascoa.production.application.port.out.OrdemRepositoryPort;
import br.com.seuprojeto.pascoa.production.application.port.out.ProducaoEventPublisherPort;
import br.com.seuprojeto.pascoa.production.application.usecase.ProducaoUseCaseImpl;
import br.com.seuprojeto.pascoa.production.domain.exception.OrdemNotFoundException;
import br.com.seuprojeto.pascoa.production.domain.exception.TransicaoOrdemInvalidaException;
import br.com.seuprojeto.pascoa.production.domain.model.ItemOrdem;
import br.com.seuprojeto.pascoa.production.domain.model.OrdemProducao;
import br.com.seuprojeto.pascoa.production.domain.model.StatusOrdem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProducaoUseCaseTest {

    @Mock OrdemRepositoryPort repository;
    @Mock ProducaoEventPublisherPort eventPublisher;

    private ProducaoUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ProducaoUseCaseImpl(repository, eventPublisher);
    }

    @Test
    @DisplayName("Criar ordem de produção salva com status PENDENTE")
    void criarOrdem() {
        when(repository.save(any())).thenAnswer(inv -> {
            OrdemProducao o = inv.getArgument(0);
            return o.withId(1L);
        });

        OrdemProducao criada = useCase.criar(new CriarOrdemCommand(
                10L, "Ana", List.of(
                        ItemOrdem.builder().produtoId(1L).nomeProduto("Ovo Trufado").quantidade(2).build()
                ), LocalDate.now().plusDays(3)));

        assertThat(criada.getStatus()).isEqualTo(StatusOrdem.PENDENTE);
        assertThat(criada.getPedidoId()).isEqualTo(10L);
        assertThat(criada.getItens()).hasSize(1);
    }

    @Test
    @DisplayName("Iniciar ordem transiciona para EM_ANDAMENTO e publica evento")
    void iniciarOrdem() {
        OrdemProducao ordem = ordemBase();
        when(repository.findById(1L)).thenReturn(Optional.of(ordem));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrdemProducao iniciada = useCase.iniciar(1L);

        assertThat(iniciada.getStatus()).isEqualTo(StatusOrdem.EM_ANDAMENTO);
        verify(eventPublisher).publishOrdemIniciada(iniciada);
    }

    @Test
    @DisplayName("Concluir ordem publica evento production.completed")
    void concluirOrdem() {
        OrdemProducao emAndamento = ordemBase().withStatus(StatusOrdem.EM_ANDAMENTO);
        when(repository.findById(1L)).thenReturn(Optional.of(emAndamento));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrdemProducao concluida = useCase.concluir(1L);

        assertThat(concluida.getStatus()).isEqualTo(StatusOrdem.CONCLUIDA);
        assertThat(concluida.estaFinalizada()).isTrue();
        verify(eventPublisher).publishOrdemConcluida(concluida);
    }

    @Test
    @DisplayName("Transição inválida CONCLUIDA → EM_ANDAMENTO lança exceção")
    void transicaoInvalida() {
        OrdemProducao concluida = ordemBase().withStatus(StatusOrdem.CONCLUIDA);
        assertThatThrownBy(() -> concluida.transicionarPara(StatusOrdem.EM_ANDAMENTO))
                .isInstanceOf(TransicaoOrdemInvalidaException.class);
    }

    @Test
    @DisplayName("buscarPorId inexistente lança OrdemNotFoundException")
    void buscarInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.buscarPorId(99L))
                .isInstanceOf(OrdemNotFoundException.class);
    }

    @Test
    @DisplayName("Fluxo completo: PENDENTE → EM_ANDAMENTO → CONCLUIDA")
    void fluxoCompleto() {
        StatusOrdem s = StatusOrdem.PENDENTE;
        assertThat(s.podeTransicionarPara(StatusOrdem.EM_ANDAMENTO)).isTrue();
        s = StatusOrdem.EM_ANDAMENTO;
        assertThat(s.podeTransicionarPara(StatusOrdem.CONCLUIDA)).isTrue();
        s = StatusOrdem.CONCLUIDA;
        assertThat(s.podeTransicionarPara(StatusOrdem.PENDENTE)).isFalse();
    }

    private OrdemProducao ordemBase() {
        return OrdemProducao.builder()
                .id(1L).pedidoId(10L).nomeCliente("Ana")
                .status(StatusOrdem.PENDENTE)
                .itens(List.of())
                .build();
    }
}
