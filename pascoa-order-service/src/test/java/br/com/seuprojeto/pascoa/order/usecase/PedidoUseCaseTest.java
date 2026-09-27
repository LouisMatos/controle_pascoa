package br.com.seuprojeto.pascoa.order.usecase;

import br.com.seuprojeto.pascoa.order.application.port.in.PedidoUseCase.*;
import br.com.seuprojeto.pascoa.order.application.port.out.*;
import br.com.seuprojeto.pascoa.order.application.usecase.PedidoUseCaseImpl;
import br.com.seuprojeto.pascoa.order.domain.exception.PedidoNotFoundException;
import br.com.seuprojeto.pascoa.order.domain.exception.TransicaoInvalidaException;
import br.com.seuprojeto.pascoa.order.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoUseCaseTest {

    @Mock PedidoRepositoryPort pedidoRepository;
    @Mock PedidoEventPublisherPort eventPublisher;
    @Mock ClienteServicePort clienteService;
    @Mock ProdutoServicePort produtoService;

    private PedidoUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new PedidoUseCaseImpl(pedidoRepository, eventPublisher, clienteService, produtoService);
    }

    @Test
    @DisplayName("Criar pedido busca cliente, salva e publica evento")
    void criarPedido() {
        when(clienteService.findById(1L))
                .thenReturn(Optional.of(new ClienteServicePort.ClienteInfo(1L, "Ana", "ana@e.com")));
        when(pedidoRepository.save(any())).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            return p.withId(10L);
        });

        Pedido criado = useCase.criar(new CriarPedidoCommand(1L, "Entrega urgente"));

        assertThat(criado.getStatus()).isEqualTo(StatusPedido.NOVO);
        assertThat(criado.getNomeCliente()).isEqualTo("Ana");
        assertThat(criado.getTokenRastreamento()).isNotBlank();
        verify(eventPublisher).publishPedidoCriado(criado);
    }

    @Test
    @DisplayName("Confirmar pedido sem itens lança IllegalStateException")
    void confirmarSemItens() {
        Pedido pedido = pedidoVazio();
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> useCase.confirmar(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sem itens");
    }

    @Test
    @DisplayName("Transição inválida ENTREGUE → CANCELADO lança TransicaoInvalidaException")
    void transicaoInvalida() {
        Pedido entregue = pedidoVazio().withStatus(StatusPedido.ENTREGUE);
        assertThatThrownBy(() -> entregue.transicionarPara(StatusPedido.CANCELADO))
                .isInstanceOf(TransicaoInvalidaException.class);
    }

    @Test
    @DisplayName("Máquina de estados: NOVO → CONFIRMADO → EM_PRODUCAO → PRONTO → ENTREGUE")
    void fluxoCompleto() {
        Pedido p = pedidoVazio();
        assertThat(p.getStatus().podeTransicionarPara(StatusPedido.CONFIRMADO)).isTrue();
        Pedido confirmado = p.transicionarPara(StatusPedido.CONFIRMADO);
        assertThat(confirmado.getStatus().podeTransicionarPara(StatusPedido.EM_PRODUCAO)).isTrue();
        Pedido emProd = confirmado.transicionarPara(StatusPedido.EM_PRODUCAO);
        Pedido pronto = emProd.transicionarPara(StatusPedido.PRONTO);
        Pedido entregue = pronto.transicionarPara(StatusPedido.ENTREGUE);
        assertThat(entregue.estaFinalizado()).isTrue();
    }

    @Test
    @DisplayName("Adicionar item a pedido CONFIRMADO lança IllegalStateException")
    void adicionarItemPedidoNaoNovo() {
        Pedido confirmado = pedidoVazio().withStatus(StatusPedido.CONFIRMADO).withId(1L);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(confirmado));

        assertThatThrownBy(() -> useCase.adicionarItem(new AdicionarItemCommand(1L, 5L, 2)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("buscarPorId inexistente lança PedidoNotFoundException")
    void buscarInexistente() {
        when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.buscarPorId(99L))
                .isInstanceOf(PedidoNotFoundException.class);
    }

    @Test
    @DisplayName("Total do pedido soma subtotais dos itens corretamente")
    void totalPedido() {
        ItemPedido item1 = ItemPedido.builder()
                .produtoId(1L).nomeProduto("Ovo A").precoUnitario(new BigDecimal("50.00")).quantidade(2).build();
        ItemPedido item2 = ItemPedido.builder()
                .produtoId(2L).nomeProduto("Ovo B").precoUnitario(new BigDecimal("30.00")).quantidade(1).build();

        Pedido pedido = pedidoVazio().withItens(java.util.List.of(item1, item2));
        assertThat(pedido.total()).isEqualByComparingTo("130.00");
    }

    private Pedido pedidoVazio() {
        return Pedido.builder()
                .id(1L).clienteId(1L).nomeCliente("Ana")
                .status(StatusPedido.NOVO)
                .tokenRastreamento("ABC123")
                .build();
    }
}
