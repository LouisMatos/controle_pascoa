package br.com.seuprojeto.pascoa.inventory.usecase;

import br.com.seuprojeto.pascoa.inventory.application.port.in.EstoqueUseCase.CriarMateriaPrimaCommand;
import br.com.seuprojeto.pascoa.inventory.application.port.in.EstoqueUseCase.MovimentarEstoqueCommand;
import br.com.seuprojeto.pascoa.inventory.application.port.out.EstoqueEventPublisherPort;
import br.com.seuprojeto.pascoa.inventory.application.port.out.MateriaPrimaRepositoryPort;
import br.com.seuprojeto.pascoa.inventory.application.port.out.MovimentacaoRepositoryPort;
import br.com.seuprojeto.pascoa.inventory.application.usecase.EstoqueUseCaseImpl;
import br.com.seuprojeto.pascoa.inventory.domain.exception.EstoqueInsuficienteException;
import br.com.seuprojeto.pascoa.inventory.domain.exception.MateriaPrimaNotFoundException;
import br.com.seuprojeto.pascoa.inventory.domain.model.MateriaPrima;
import br.com.seuprojeto.pascoa.inventory.domain.model.Movimentacao;
import br.com.seuprojeto.pascoa.inventory.domain.model.TipoMovimentacao;
import br.com.seuprojeto.pascoa.inventory.domain.model.Unidade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EstoqueUseCaseTest {

    @Mock MateriaPrimaRepositoryPort materiaPrimaRepository;
    @Mock MovimentacaoRepositoryPort movimentacaoRepository;
    @Mock EstoqueEventPublisherPort eventPublisher;

    private EstoqueUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new EstoqueUseCaseImpl(materiaPrimaRepository, movimentacaoRepository, eventPublisher);
    }

    @Test
    @DisplayName("Criar matéria-prima com estoque inicial salva corretamente")
    void criarMateriaPrima() {
        when(materiaPrimaRepository.save(any())).thenAnswer(inv -> {
            MateriaPrima mp = inv.getArgument(0);
            return mp.withId(1L);
        });

        MateriaPrima criada = useCase.criar(new CriarMateriaPrimaCommand(
                "Chocolate", Unidade.KG, new BigDecimal("10.000"),
                new BigDecimal("2.000"), null));

        assertThat(criada.getNome()).isEqualTo("Chocolate");
        assertThat(criada.getQuantidadeEstoque()).isEqualByComparingTo("10.000");
        assertThat(criada.isAtivo()).isTrue();
    }

    @Test
    @DisplayName("Entrada aumenta estoque corretamente")
    void registrarEntrada() {
        MateriaPrima mp = materiaPrimaBase("5.000");
        when(materiaPrimaRepository.findById(1L)).thenReturn(Optional.of(mp));
        when(materiaPrimaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(movimentacaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Movimentacao mov = useCase.registrarEntrada(
                new MovimentarEstoqueCommand(1L, new BigDecimal("3.000"), "Compra fornecedor"));

        assertThat(mov.getTipo()).isEqualTo(TipoMovimentacao.ENTRADA);
        verify(materiaPrimaRepository).save(argThat(m ->
                m.getQuantidadeEstoque().compareTo(new BigDecimal("8.000")) == 0));
    }

    @Test
    @DisplayName("Saída com estoque suficiente reduz e gera evento de saída")
    void registrarSaidaSucesso() {
        MateriaPrima mp = materiaPrimaBase("5.000");
        when(materiaPrimaRepository.findById(1L)).thenReturn(Optional.of(mp));
        when(materiaPrimaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(movimentacaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.registrarSaida(new MovimentarEstoqueCommand(1L, new BigDecimal("2.000"), "Produção"));

        verify(eventPublisher).publishSaidaRealizada(eq(1L), eq(new BigDecimal("2.000")), any());
    }

    @Test
    @DisplayName("Saída com estoque insuficiente lança EstoqueInsuficienteException")
    void registrarSaidaInsuficiente() {
        MateriaPrima mp = materiaPrimaBase("1.000");
        when(materiaPrimaRepository.findById(1L)).thenReturn(Optional.of(mp));

        assertThatThrownBy(() -> useCase.registrarSaida(
                new MovimentarEstoqueCommand(1L, new BigDecimal("5.000"), null)))
                .isInstanceOf(EstoqueInsuficienteException.class);
    }

    @Test
    @DisplayName("buscarPorId com id inexistente lança MateriaPrimaNotFoundException")
    void buscarInexistente() {
        when(materiaPrimaRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.buscarPorId(99L))
                .isInstanceOf(MateriaPrimaNotFoundException.class);
    }

    @Test
    @DisplayName("estoqueEstaCritico detecta quando quantidade <= mínimo")
    void estoqueCritico() {
        MateriaPrima mp = materiaPrimaBase("1.500"); // quantidade <= estoqueMinimo(2.000)
        assertThat(mp.estoqueEstaCritico()).isTrue();

        MateriaPrima ok = materiaPrimaBase("5.000");
        assertThat(ok.estoqueEstaCritico()).isFalse();
    }

    private MateriaPrima materiaPrimaBase(String quantidade) {
        return MateriaPrima.builder()
                .id(1L).nome("Chocolate").unidade(Unidade.KG)
                .quantidadeEstoque(new BigDecimal(quantidade))
                .estoqueMinimo(new BigDecimal("2.000"))
                .ativo(true).build();
    }
}
