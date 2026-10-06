package br.com.seuprojeto.pascoa.pedido.service;

import br.com.seuprojeto.pascoa.cadastro.entity.Cliente;
import br.com.seuprojeto.pascoa.cadastro.entity.PreferenciaCanal;
import br.com.seuprojeto.pascoa.cadastro.entity.Produto;
import br.com.seuprojeto.pascoa.cadastro.entity.UnidadeVenda;
import br.com.seuprojeto.pascoa.cadastro.repository.ClienteRepository;
import br.com.seuprojeto.pascoa.cadastro.repository.ProdutoRepository;
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
}
