package br.com.seuprojeto.pascoa.pedido.service;

import br.com.seuprojeto.pascoa.cadastro.entity.Cliente;
import br.com.seuprojeto.pascoa.cadastro.entity.PreferenciaCanal;
import br.com.seuprojeto.pascoa.cadastro.entity.Produto;
import br.com.seuprojeto.pascoa.cadastro.entity.UnidadeVenda;
import br.com.seuprojeto.pascoa.cadastro.repository.ClienteRepository;
import br.com.seuprojeto.pascoa.cadastro.repository.ProdutoRepository;
import br.com.seuprojeto.pascoa.orcamento.dto.OrcamentoForm;
import br.com.seuprojeto.pascoa.orcamento.dto.OrcamentoItemForm;
import br.com.seuprojeto.pascoa.orcamento.service.OrcamentoService;
import br.com.seuprojeto.pascoa.pedido.entity.Pedido;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@WithMockUser(roles = "ADMIN")
class TotalMaximoTest {

    private static final String MENSAGEM = "Total acima do máximo permitido.";

    @Autowired private PedidoService pedidoService;
    @Autowired private ClienteRepository clientes;
    @Autowired private ProdutoRepository produtos;
    @Autowired private OrcamentoService orcamentoService;
    @Autowired private JdbcTemplate jdbc;

    private Cliente cliente() {
        return clientes.save(Cliente.builder().nome("C-" + UUID.randomUUID()).optIn(false)
            .preferenciaCanal(PreferenciaCanal.NENHUM).build());
    }

    private Produto bolo() {
        return produtos.save(Produto.builder().nome("Bolo-" + UUID.randomUUID())
            .precoVenda(new BigDecimal("40.00")).unidadeVenda(UnidadeVenda.KG).build());
    }

    private int pedidosDoCliente(Long clienteId) {
        return jdbc.queryForObject("SELECT count(*) FROM pedidos WHERE cliente_id = ?", Integer.class, clienteId);
    }

    @Test
    void criarComItens_totalAcimaDoMaximo_rejeitaESemPersistir() {
        Cliente c = cliente();
        Produto bolo = bolo();

        assertThatThrownBy(() -> pedidoService.criarComItens(c.getId(), LocalDate.now().plusDays(3), null, null,
            List.of(bolo.getId()), List.of(new BigDecimal("9999999.999"))))
            .isInstanceOf(IllegalArgumentException.class).hasMessage(MENSAGEM);

        assertThat(pedidosDoCliente(c.getId())).isZero();
    }

    @Test
    void adicionarItem_totalAcimaDoMaximo_rejeitaEMantemPedidoIntacto() {
        Cliente c = cliente();
        Produto bolo = bolo();
        Produto outro = bolo();
        Pedido pedido = pedidoService.criarComItens(c.getId(), LocalDate.now().plusDays(3), null, null,
            List.of(bolo.getId()), List.of(new BigDecimal("1")));

        assertThatThrownBy(() -> pedidoService.adicionarItem(pedido.getId(), outro.getId(), new BigDecimal("9999999.999")))
            .isInstanceOf(IllegalArgumentException.class).hasMessage(MENSAGEM);

        assertThat(jdbc.queryForObject("SELECT total_pedido FROM pedidos WHERE id = ?", BigDecimal.class, pedido.getId()))
            .isEqualByComparingTo("40.00");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM itens_pedido WHERE pedido_id = ?", Integer.class, pedido.getId()))
            .isEqualTo(1);
    }

    private Produto caro() {
        return produtos.save(Produto.builder().nome("Caro-" + UUID.randomUUID())
            .precoVenda(new BigDecimal("2000.00")).unidadeVenda(UnidadeVenda.KG).build());
    }

    @Test
    void criarComItens_linhaEnormeAcimaDoMaximo_rejeitaComMensagemAmigavelESemPersistir() {
        Cliente c = cliente();
        Produto caro = caro();

        assertThatThrownBy(() -> pedidoService.criarComItens(c.getId(), LocalDate.now().plusDays(3), null, null,
            List.of(caro.getId()), List.of(new BigDecimal("9999999"))))
            .isInstanceOf(IllegalArgumentException.class).hasMessage(MENSAGEM);

        assertThat(pedidosDoCliente(c.getId())).isZero();
        assertThat(jdbc.queryForObject(
            "SELECT count(*) FROM itens_pedido i JOIN pedidos p ON p.id = i.pedido_id WHERE p.cliente_id = ?",
            Integer.class, c.getId())).isZero();
    }

    @Test
    void criarComItens_produtoCaroComQuantidadeNormal_funciona() {
        Cliente c = cliente();
        Produto caro = caro();

        Pedido p = pedidoService.criarComItens(c.getId(), LocalDate.now().plusDays(3), null, null,
            List.of(caro.getId()), List.of(new BigDecimal("2")));

        assertThat(p.getTotalPedido()).isEqualByComparingTo("4000.00");
    }

    private OrcamentoItemForm itemOrc(Produto p, String qtd) {
        OrcamentoItemForm f = new OrcamentoItemForm();
        f.setProdutoId(p.getId());
        f.setQuantidade(new BigDecimal(qtd));
        return f;
    }

    @Test
    void orcamentoCriar_linhaEnormeDepoisDeLinhasNormais_rejeitaESemPersistir() {
        Cliente c = cliente();
        OrcamentoForm form = new OrcamentoForm();
        form.setClienteId(c.getId());
        form.setValidade(LocalDate.now().plusDays(7));
        form.setItens(List.of(itemOrc(bolo(), "1"), itemOrc(bolo(), "2"), itemOrc(caro(), "9999999")));

        assertThatThrownBy(() -> orcamentoService.criar(form, "teste"))
            .isInstanceOf(IllegalArgumentException.class).hasMessage(MENSAGEM);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM orcamentos WHERE cliente_id = ?", Integer.class, c.getId()))
            .isZero();
        assertThat(jdbc.queryForObject(
            "SELECT count(*) FROM orcamento_itens i JOIN orcamentos o ON o.id = i.orcamento_id WHERE o.cliente_id = ?",
            Integer.class, c.getId())).isZero();
    }
}
