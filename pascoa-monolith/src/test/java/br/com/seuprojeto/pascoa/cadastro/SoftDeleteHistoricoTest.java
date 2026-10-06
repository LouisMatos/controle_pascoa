package br.com.seuprojeto.pascoa.cadastro;

import br.com.seuprojeto.pascoa.cadastro.entity.Cliente;
import br.com.seuprojeto.pascoa.cadastro.entity.PreferenciaCanal;
import br.com.seuprojeto.pascoa.cadastro.entity.Produto;
import br.com.seuprojeto.pascoa.cadastro.repository.ClienteRepository;
import br.com.seuprojeto.pascoa.cadastro.repository.ProdutoRepository;
import br.com.seuprojeto.pascoa.cadastro.service.ClienteService;
import br.com.seuprojeto.pascoa.cadastro.service.ProdutoService;
import br.com.seuprojeto.pascoa.pedido.dto.PedidoForm;
import br.com.seuprojeto.pascoa.pedido.entity.Pedido;
import br.com.seuprojeto.pascoa.pedido.service.PedidoService;
import br.com.seuprojeto.pascoa.shared.exception.RecursoNaoEncontradoException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@WithMockUser(roles = "ADMIN")
class SoftDeleteHistoricoTest {

    @Autowired private PedidoService pedidoService;
    @Autowired private ClienteService clienteService;
    @Autowired private ProdutoService produtoService;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private ProdutoRepository produtoRepository;
    @Autowired private EntityManager em;

    private Cliente cliente;
    private Produto produto;

    @BeforeEach
    void setUp() {
        cliente = clienteRepository.save(Cliente.builder()
                .nome("Cliente Soft Delete")
                .optIn(false)
                .preferenciaCanal(PreferenciaCanal.NENHUM)
                .build());
        produto = produtoRepository.save(Produto.builder()
                .nome("Ovo Soft Delete")
                .precoVenda(new BigDecimal("50.00"))
                .ativo(true)
                .build());
        em.flush();
        em.clear();
    }

    private Long pedidoComItem() {
        Pedido pedido = pedidoService.criarComItens(
                cliente.getId(), LocalDate.now().plusDays(2), null, null,
                List.of(produto.getId()), List.of(1));
        em.flush();
        em.clear();
        return pedido.getId();
    }

    @Test
    @DisplayName("Pedido de cliente e produto excluídos continua legível, com os nomes do histórico")
    void historicoDeRegistroExcluido_continuaLegivel() {
        Long pedidoId = pedidoComItem();
        clienteService.excluir(cliente.getId());
        produtoService.excluir(produto.getId());
        em.flush();
        em.clear();

        assertThatCode(() -> {
            Pedido pedido = pedidoService.buscarPorId(pedidoId);
            assertThat(pedido.getCliente().getNome()).isEqualTo("Cliente Soft Delete");
            assertThat(pedido.getItens().get(0).getProduto().getNome()).isEqualTo("Ovo Soft Delete");
        }).doesNotThrowAnyException();

        assertThatCode(() -> pedidoService.listarTodos()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Cliente e produto excluídos saem das listagens")
    void excluidos_saemDasListagens() {
        clienteService.excluir(cliente.getId());
        produtoService.excluir(produto.getId());
        em.flush();
        em.clear();

        assertThat(clienteService.listarTodos()).noneMatch(c -> c.getId().equals(cliente.getId()));
        assertThat(produtoService.listarTodos()).noneMatch(p -> p.getId().equals(produto.getId()));
        assertThat(produtoService.listarAtivos()).noneMatch(p -> p.getId().equals(produto.getId()));
    }

    @Test
    @DisplayName("Novo pedido não aceita cliente excluído")
    void novoPedido_comClienteExcluido_recusado() {
        clienteService.excluir(cliente.getId());
        em.flush();
        em.clear();

        PedidoForm form = new PedidoForm();
        form.setClienteId(cliente.getId());
        form.setDataEntrega(LocalDate.now().plusDays(3));

        assertThatThrownBy(() -> pedidoService.criar(form))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("Item novo não aceita produto excluído")
    void novoItem_comProdutoExcluido_recusado() {
        Long pedidoId = pedidoComItem();
        Produto outro = produtoRepository.save(Produto.builder()
                .nome("Ovo Excluido")
                .precoVenda(new BigDecimal("70.00"))
                .ativo(true)
                .build());
        em.flush();
        produtoService.excluir(outro.getId());
        em.flush();
        em.clear();

        assertThatThrownBy(() -> pedidoService.adicionarItem(pedidoId, outro.getId(), 1))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
