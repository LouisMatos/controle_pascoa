package br.com.seuprojeto.pascoa.pedido.service;

import br.com.seuprojeto.pascoa.cadastro.entity.Categoria;
import br.com.seuprojeto.pascoa.cadastro.entity.Cliente;
import br.com.seuprojeto.pascoa.cadastro.entity.PreferenciaCanal;
import br.com.seuprojeto.pascoa.cadastro.entity.Produto;
import br.com.seuprojeto.pascoa.cadastro.repository.ClienteRepository;
import br.com.seuprojeto.pascoa.cadastro.repository.ProdutoRepository;
import br.com.seuprojeto.pascoa.gastos.entity.CategoriaGasto;
import br.com.seuprojeto.pascoa.gastos.entity.GastoVariavel;
import br.com.seuprojeto.pascoa.gastos.repository.GastoVariavelRepository;
import br.com.seuprojeto.pascoa.pedido.entity.Pedido;
import br.com.seuprojeto.pascoa.pedido.entity.StatusPedido;
import br.com.seuprojeto.pascoa.pedido.repository.PedidoRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes de integração para a máquina de estados de {@link PedidoService}.
 *
 * <p>Cobre transições válidas, transições inválidas (lançam exceção)
 * e efeito colateral do cancelamento (F6: desconsiderarNoCusto em gastos vinculados).
 *
 * <p>Todas as chamadas de mutação usam {@code @WithMockUser(roles = "ADMIN")} porque
 * {@code @PreAuthorize("@authService.owns(...)")} exige autenticação não-anônima.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PedidoStateMachineTest {

    @Autowired private PedidoService          pedidoService;
    @Autowired private ClienteRepository      clienteRepository;
    @Autowired private ProdutoRepository      produtoRepository;
    @Autowired private PedidoRepository       pedidoRepository;
    @Autowired private GastoVariavelRepository gastoRepository;
    @Autowired private EntityManager          em;

    private Cliente cliente;
    private Produto produto;

    @BeforeEach
    void setUp() {
        cliente = clienteRepository.save(Cliente.builder()
                .nome("Cliente Teste SM")
                .optIn(false)
                .preferenciaCanal(PreferenciaCanal.NENHUM)
                .build());

        produto = produtoRepository.save(Produto.builder()
                .nome("Ovo Teste SM")
                .categoria(Categoria.TRADICIONAL)
                .precoVenda(new BigDecimal("50.00"))
                .ativo(true)
                .build());
    }

    /**
     * Cria pedido NOVO com um item via criarComItens.
     *
     * <p>Faz flush+clear após a criação porque {@code criarComItens()} salva os
     * {@code ItemPedido} via {@code itemRepository.save()} sem adicioná-los à
     * coleção em memória do {@code Pedido}. Sem o clear, o Hibernate retorna
     * o objeto cached (sem itens) quando {@code confirmar()} chama
     * {@code findByIdComItens()}, causando falso "Pedido sem itens".
     */
    private Pedido pedidoNovo() {
        Pedido pedido = pedidoService.criarComItens(
                cliente.getId(),
                LocalDate.now().plusDays(7),
                null,
                "Pedido de teste",
                List.of(produto.getId()),
                List.of(2));
        em.flush();
        em.clear();
        return pedido;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Transições válidas
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("NOVO → confirmar → status CONFIRMADO")
    @WithMockUser(roles = "ADMIN")
    void confirmar_deNovo_ficaConfirmado() {
        Pedido pedido = pedidoNovo();

        Pedido confirmado = pedidoService.confirmar(pedido.getId());

        assertThat(confirmado.getStatus()).isEqualTo(StatusPedido.CONFIRMADO);
    }

    @Test
    @DisplayName("CONFIRMADO → cancelar → status CANCELADO")
    @WithMockUser(roles = "ADMIN")
    void cancelar_deConfirmado_ficaCancelado() {
        Pedido pedido = pedidoNovo();
        pedidoService.confirmar(pedido.getId());
        em.flush();
        em.clear();

        Pedido cancelado = pedidoService.cancelar(pedido.getId());

        assertThat(cancelado.getStatus()).isEqualTo(StatusPedido.CANCELADO);
    }

    @Test
    @DisplayName("NOVO → cancelar → status CANCELADO (cancela sem confirmar)")
    @WithMockUser(roles = "ADMIN")
    void cancelar_deNovo_ficaCancelado() {
        Pedido pedido = pedidoNovo();

        Pedido cancelado = pedidoService.cancelar(pedido.getId());

        assertThat(cancelado.getStatus()).isEqualTo(StatusPedido.CANCELADO);
    }

    @Test
    @DisplayName("CONFIRMADO → marcarPronto → status PRONTO")
    @WithMockUser(roles = "ADMIN")
    void marcarPronto_deConfirmado_ficaPronto() {
        Pedido pedido = pedidoNovo();
        pedidoService.confirmar(pedido.getId());
        em.flush();
        em.clear();

        Pedido pronto = pedidoService.marcarPronto(pedido.getId());

        assertThat(pronto.getStatus()).isEqualTo(StatusPedido.PRONTO);
    }

    @Test
    @DisplayName("PRONTO → entregar → status ENTREGUE")
    @WithMockUser(roles = "ADMIN")
    void entregar_dePronto_ficaEntregue() {
        Pedido pedido = pedidoNovo();
        pedidoService.confirmar(pedido.getId());
        em.flush(); em.clear();
        pedidoService.marcarPronto(pedido.getId());
        em.flush(); em.clear();

        Pedido entregue = pedidoService.registrarEntrega(pedido.getId());

        assertThat(entregue.getStatus()).isEqualTo(StatusPedido.ENTREGUE);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Transições inválidas
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Confirmar pedido CONFIRMADO lança IllegalStateException")
    @WithMockUser(roles = "ADMIN")
    void confirmar_jaConfirmado_lancaExcecao() {
        Pedido pedido = pedidoNovo();
        pedidoService.confirmar(pedido.getId());
        em.flush(); em.clear();

        assertThatThrownBy(() -> pedidoService.confirmar(pedido.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("NOVO");
    }

    @Test
    @DisplayName("Confirmar pedido sem itens lança IllegalStateException")
    @WithMockUser(roles = "ADMIN")
    void confirmar_semItens_lancaExcecao() {
        // Cria pedido vazio diretamente no repositório (sem itens)
        Pedido pedidoVazio = pedidoRepository.save(Pedido.builder()
                .cliente(cliente)
                .totalPedido(BigDecimal.ZERO)
                .build());
        em.flush(); em.clear();

        assertThatThrownBy(() -> pedidoService.confirmar(pedidoVazio.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sem itens");
    }

    @Test
    @DisplayName("Cancelar pedido ENTREGUE lança IllegalStateException")
    @WithMockUser(roles = "ADMIN")
    void cancelar_entregue_lancaExcecao() {
        Pedido pedido = pedidoNovo();
        pedidoService.confirmar(pedido.getId());
        em.flush(); em.clear();
        pedidoService.marcarPronto(pedido.getId());
        em.flush(); em.clear();
        pedidoService.registrarEntrega(pedido.getId());
        em.flush(); em.clear();

        assertThatThrownBy(() -> pedidoService.cancelar(pedido.getId()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Cancelar pedido já CANCELADO lança IllegalStateException")
    @WithMockUser(roles = "ADMIN")
    void cancelar_jaCancelado_lancaExcecao() {
        Pedido pedido = pedidoNovo();
        pedidoService.cancelar(pedido.getId());
        em.flush(); em.clear();

        assertThatThrownBy(() -> pedidoService.cancelar(pedido.getId()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("marcarPronto a partir de NOVO lança IllegalStateException")
    @WithMockUser(roles = "ADMIN")
    void marcarPronto_deNovo_lancaExcecao() {
        Pedido pedido = pedidoNovo();

        assertThatThrownBy(() -> pedidoService.marcarPronto(pedido.getId()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("registrarEntrega a partir de CONFIRMADO (não PRONTO) lança IllegalStateException")
    @WithMockUser(roles = "ADMIN")
    void registrarEntrega_naoEstaPronto_lancaExcecao() {
        Pedido pedido = pedidoNovo();
        pedidoService.confirmar(pedido.getId());
        em.flush(); em.clear();

        assertThatThrownBy(() -> pedidoService.registrarEntrega(pedido.getId()))
                .isInstanceOf(IllegalStateException.class);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // F6 — cancelar pedido desconsidere gastos vinculados
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("F6: Cancelar pedido marca gastos vinculados como desconsiderarNoCusto=true")
    @WithMockUser(roles = "ADMIN")
    void cancelar_desconsideraGastosVinculados() {
        Pedido pedido = pedidoNovo();
        Long pedidoId = pedido.getId();

        // Salva 2 gastos com pedidoId vinculado
        gastoRepository.save(GastoVariavel.builder()
                .descricao("Embalagem especial")
                .valor(new BigDecimal("30.00"))
                .dataLancamento(LocalDate.now())
                .categoria(CategoriaGasto.EMBALAGEM)
                .pedidoId(pedidoId)
                .build());
        gastoRepository.save(GastoVariavel.builder()
                .descricao("Transporte expresso")
                .valor(new BigDecimal("20.00"))
                .dataLancamento(LocalDate.now())
                .categoria(CategoriaGasto.TRANSPORTE)
                .pedidoId(pedidoId)
                .build());
        // Gasto sem vínculo — não deve ser afetado
        gastoRepository.save(GastoVariavel.builder()
                .descricao("Gasto geral sem pedido")
                .valor(new BigDecimal("50.00"))
                .dataLancamento(LocalDate.now())
                .categoria(CategoriaGasto.OUTROS)
                .build());

        em.flush(); em.clear();

        pedidoService.cancelar(pedidoId);
        em.flush(); em.clear();

        // Gastos vinculados devem estar marcados
        var gastosVinculados = gastoRepository.findAll().stream()
                .filter(g -> pedidoId.equals(g.getPedidoId()))
                .toList();
        assertThat(gastosVinculados).isNotEmpty();
        assertThat(gastosVinculados).allSatisfy(g ->
                assertThat(g.getDesconsiderarNoCusto()).isTrue());

        // Gasto sem vínculo NÃO deve ser marcado
        var semVinculo = gastoRepository.findAll().stream()
                .filter(g -> g.getPedidoId() == null)
                .toList();
        assertThat(semVinculo).allSatisfy(g ->
                assertThat(g.getDesconsiderarNoCusto()).isFalse());
    }

    @Test
    @DisplayName("B9: sumTotal exclui gastos com desconsiderarNoCusto=true")
    void sumTotal_excluiDesconsiderados() {
        int ano = LocalDate.now().getYear();
        int mes = LocalDate.now().getMonthValue();

        gastoRepository.save(GastoVariavel.builder()
                .descricao("Normal")
                .valor(new BigDecimal("100.00"))
                .dataLancamento(LocalDate.now())
                .categoria(CategoriaGasto.OUTROS)
                .build());

        GastoVariavel desconsiderado = GastoVariavel.builder()
                .descricao("Cancelado")
                .valor(new BigDecimal("999.00"))
                .dataLancamento(LocalDate.now())
                .categoria(CategoriaGasto.OUTROS)
                .build();
        desconsiderado.setDesconsiderarNoCusto(true);
        gastoRepository.save(desconsiderado);

        em.flush(); em.clear();

        BigDecimal soma = gastoRepository.sumTotal(ano, mes);
        assertThat(soma).isEqualByComparingTo(new BigDecimal("100.00"));
    }
}
