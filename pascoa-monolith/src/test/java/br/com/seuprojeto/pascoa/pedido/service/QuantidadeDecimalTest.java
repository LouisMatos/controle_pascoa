package br.com.seuprojeto.pascoa.pedido.service;

import br.com.seuprojeto.pascoa.cadastro.entity.Cliente;
import br.com.seuprojeto.pascoa.cadastro.entity.PreferenciaCanal;
import br.com.seuprojeto.pascoa.cadastro.entity.Produto;
import br.com.seuprojeto.pascoa.cadastro.entity.UnidadeVenda;
import br.com.seuprojeto.pascoa.cadastro.repository.ClienteRepository;
import br.com.seuprojeto.pascoa.cadastro.repository.ProdutoRepository;
import br.com.seuprojeto.pascoa.pedido.entity.Pedido;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@WithMockUser(roles = "ADMIN")
class QuantidadeDecimalTest {

    @Autowired private PedidoService pedidoService;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private ProdutoRepository produtos;
    @Autowired private EntityManager em;

    private Cliente cliente;

    @BeforeEach
    void setUp() {
        cliente = clienteRepository.save(Cliente.builder()
                .nome("Cliente Decimal")
                .optIn(false)
                .preferenciaCanal(PreferenciaCanal.NENHUM)
                .build());
    }

    @Test
    void itemEmKg_aceitaFracao_eCalculaSubtotalEmDuasCasas() {
        Produto bolo = produtos.save(Produto.builder().nome("Bolo-" + UUID.randomUUID())
            .precoVenda(new BigDecimal("40.00")).unidadeVenda(UnidadeVenda.KG).build());

        Pedido pedido = pedidoService.criarComItens(cliente.getId(), LocalDate.now().plusDays(3), null, null,
            List.of(bolo.getId()), List.of(new BigDecimal("1.5")));
        em.flush();
        em.clear();

        Pedido lido = pedidoService.buscarPorId(pedido.getId());
        assertThat(lido.getItens()).hasSize(1);
        assertThat(lido.getItens().get(0).getQuantidade()).isEqualByComparingTo("1.5");
        assertThat(lido.getItens().get(0).getSubtotal()).isEqualByComparingTo("60.00");
        assertThat(lido.getTotalPedido()).isEqualByComparingTo("60.00");
    }

    @Test
    void itemEmCento_rejeitaFracao() {
        Produto coxinha = produtos.save(Produto.builder().nome("Coxinha-" + UUID.randomUUID())
            .precoVenda(new BigDecimal("80.00")).unidadeVenda(UnidadeVenda.CENTO).build());

        assertThatThrownBy(() -> pedidoService.criarComItens(cliente.getId(), LocalDate.now().plusDays(3), null, null,
            List.of(coxinha.getId()), List.of(new BigDecimal("1.5"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cento só aceita quantidade inteira.");
    }

    @Test
    void adicionarItem_validaPelaUnidadeDoProduto() {
        Produto brigadeiro = produtos.save(Produto.builder().nome("Brigadeiro-" + UUID.randomUUID())
            .precoVenda(new BigDecimal("30.00")).unidadeVenda(UnidadeVenda.DUZIA).build());
        Produto bolo = produtos.save(Produto.builder().nome("Bolo-" + UUID.randomUUID())
            .precoVenda(new BigDecimal("40.00")).unidadeVenda(UnidadeVenda.KG).build());
        Pedido pedido = pedidoService.criarComItens(cliente.getId(), LocalDate.now().plusDays(3), null, null,
            List.of(brigadeiro.getId()), List.of(new BigDecimal("2")));
        em.flush();
        em.clear();

        assertThatThrownBy(() -> pedidoService.adicionarItem(pedido.getId(), bolo.getId(), BigDecimal.ZERO))
            .hasMessage("Quantidade deve ser maior que zero.");
        em.clear();
        pedidoService.adicionarItem(pedido.getId(), bolo.getId(), new BigDecimal("0.75"));
        em.flush();
        em.clear();

        assertThat(pedidoService.buscarPorId(pedido.getId()).getTotalPedido()).isEqualByComparingTo("90.00");
    }

    @Test
    void arredondamentoHalfUp_totalIgualASomaDosSubtotais() {
        Produto bolo = produtos.save(Produto.builder().nome("Bolo-" + UUID.randomUUID())
            .precoVenda(new BigDecimal("40.00")).unidadeVenda(UnidadeVenda.KG).build());

        Pedido pedido = pedidoService.criarComItens(cliente.getId(), LocalDate.now().plusDays(3), null, null,
            List.of(bolo.getId()), List.of(new BigDecimal("0.333")));
        em.flush();
        em.clear();

        Pedido lido = pedidoService.buscarPorId(pedido.getId());
        assertThat(lido.getItens().get(0).getSubtotal()).isEqualByComparingTo("13.32");
        assertThat(lido.getTotalPedido()).isEqualByComparingTo("13.32");
    }

    @Test
    void removerItem_recalculaTotal_eApagaALinha() {
        Produto bolo = produtos.save(Produto.builder().nome("Bolo-" + UUID.randomUUID())
            .precoVenda(new BigDecimal("40.00")).unidadeVenda(UnidadeVenda.KG).build());
        Produto brigadeiro = produtos.save(Produto.builder().nome("Brigadeiro-" + UUID.randomUUID())
            .precoVenda(new BigDecimal("30.00")).unidadeVenda(UnidadeVenda.DUZIA).build());
        Pedido pedido = pedidoService.criarComItens(cliente.getId(), LocalDate.now().plusDays(3), null, null,
            List.of(bolo.getId(), brigadeiro.getId()), List.of(new BigDecimal("1.5"), new BigDecimal("2")));
        em.flush();
        em.clear();
        Long removido = pedidoService.buscarPorId(pedido.getId()).getItens().stream()
            .filter(i -> i.getProduto().getId().equals(bolo.getId())).findFirst().orElseThrow().getId();
        em.clear();

        pedidoService.removerItem(pedido.getId(), removido);
        em.flush();
        em.clear();

        Pedido lido = pedidoService.buscarPorId(pedido.getId());
        assertThat(lido.getItens()).hasSize(1);
        assertThat(lido.getTotalPedido()).isEqualByComparingTo("60.00");
        assertThat(em.createQuery("select count(i) from ItemPedido i where i.id = :id", Long.class)
            .setParameter("id", removido).getSingleResult()).isZero();
    }
}
