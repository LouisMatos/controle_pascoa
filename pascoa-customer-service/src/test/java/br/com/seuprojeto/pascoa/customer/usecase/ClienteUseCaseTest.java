package br.com.seuprojeto.pascoa.customer.usecase;

import br.com.seuprojeto.pascoa.customer.application.port.in.ClienteUseCase.AtualizarClienteCommand;
import br.com.seuprojeto.pascoa.customer.application.port.in.ClienteUseCase.CriarClienteCommand;
import br.com.seuprojeto.pascoa.customer.application.port.out.ClienteEventPublisherPort;
import br.com.seuprojeto.pascoa.customer.application.port.out.ClienteRepositoryPort;
import br.com.seuprojeto.pascoa.customer.application.usecase.ClienteUseCaseImpl;
import br.com.seuprojeto.pascoa.customer.domain.exception.ClienteNotFoundException;
import br.com.seuprojeto.pascoa.customer.domain.exception.EmailDuplicadoException;
import br.com.seuprojeto.pascoa.customer.domain.model.Cliente;
import br.com.seuprojeto.pascoa.customer.domain.model.PreferenciaCanal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteUseCaseTest {

    @Mock ClienteRepositoryPort repository;
    @Mock ClienteEventPublisherPort eventPublisher;

    private ClienteUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ClienteUseCaseImpl(repository, eventPublisher);
    }

    @Test
    @DisplayName("Criar cliente com e-mail novo salva e publica evento")
    void criarClienteSucesso() {
        when(repository.existsByEmail("ana@example.com")).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            return c.withId(1L);
        });

        var cmd = new CriarClienteCommand(
                "Ana", "ana@example.com", "11999999999",
                null, null, null, PreferenciaCanal.EMAIL);

        Cliente criado = useCase.criar(cmd);

        assertThat(criado.getNome()).isEqualTo("Ana");
        assertThat(criado.getPontosFidelidade()).isZero();
        assertThat(criado.isAtivo()).isTrue();
        verify(eventPublisher).publishClienteCriado(criado);
    }

    @Test
    @DisplayName("Criar cliente com e-mail duplicado lança EmailDuplicadoException")
    void criarClienteEmailDuplicado() {
        when(repository.existsByEmail("ana@example.com")).thenReturn(true);

        var cmd = new CriarClienteCommand(
                "Ana", "ana@example.com", null, null, null, null, PreferenciaCanal.EMAIL);

        assertThatThrownBy(() -> useCase.criar(cmd))
                .isInstanceOf(EmailDuplicadoException.class);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("buscarPorId com id inexistente lança ClienteNotFoundException")
    void buscarInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.buscarPorId(99L))
                .isInstanceOf(ClienteNotFoundException.class);
    }

    @Test
    @DisplayName("adicionarPontos acumula corretamente")
    void adicionarPontos() {
        Cliente existente = clienteBase().withPontosFidelidade(100);
        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Cliente atualizado = useCase.adicionarPontos(1L, 50);

        assertThat(atualizado.getPontosFidelidade()).isEqualTo(150);
    }

    @Test
    @DisplayName("inativar seta ativo=false e publica evento")
    void inativar() {
        when(repository.findById(1L)).thenReturn(Optional.of(clienteBase()));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.inativar(1L);

        verify(repository).save(argThat(c -> !c.isAtivo()));
        verify(eventPublisher).publishClienteInativado(1L);
    }

    @Test
    @DisplayName("fazcAniversarioHoje detecta aniversariantes")
    void aniversario() {
        var hoje = java.time.LocalDate.now();
        Cliente aniversariante = clienteBase().withDataNascimento(hoje.minusYears(30));
        assertThat(aniversariante.fazcAniversarioHoje()).isTrue();
    }

    private Cliente clienteBase() {
        return Cliente.builder()
                .id(1L).nome("Ana").email("ana@example.com")
                .preferenciaCanal(PreferenciaCanal.EMAIL)
                .pontosFidelidade(0).ativo(true).build();
    }
}
