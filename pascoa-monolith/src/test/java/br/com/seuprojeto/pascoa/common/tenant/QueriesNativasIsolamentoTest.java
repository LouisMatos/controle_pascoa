package br.com.seuprojeto.pascoa.common.tenant;

import br.com.seuprojeto.pascoa.cadastro.entity.Categoria;
import br.com.seuprojeto.pascoa.cadastro.entity.Cliente;
import br.com.seuprojeto.pascoa.cadastro.entity.PreferenciaCanal;
import br.com.seuprojeto.pascoa.cadastro.entity.Produto;
import br.com.seuprojeto.pascoa.cadastro.repository.ClienteRepository;
import br.com.seuprojeto.pascoa.cadastro.repository.ProdutoRepository;
import br.com.seuprojeto.pascoa.crm.entity.PontoFidelidade;
import br.com.seuprojeto.pascoa.crm.entity.TipoPonto;
import br.com.seuprojeto.pascoa.crm.repository.PontoFidelidadeRepository;
import br.com.seuprojeto.pascoa.notificacao.entity.CanalNotificacao;
import br.com.seuprojeto.pascoa.notificacao.entity.EventoNotificacao;
import br.com.seuprojeto.pascoa.notificacao.entity.NotificacaoEnviada;
import br.com.seuprojeto.pascoa.notificacao.entity.StatusEnvio;
import br.com.seuprojeto.pascoa.notificacao.repository.NotificacaoEnviadaRepository;
import br.com.seuprojeto.pascoa.pedido.entity.ItemPedido;
import br.com.seuprojeto.pascoa.pedido.entity.Pedido;
import br.com.seuprojeto.pascoa.pedido.entity.StatusPedido;
import br.com.seuprojeto.pascoa.pedido.repository.ItemPedidoRepository;
import br.com.seuprojeto.pascoa.pedido.repository.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class QueriesNativasIsolamentoTest {

    @Autowired private ClienteRepository clientes;
    @Autowired private ProdutoRepository produtos;
    @Autowired private PedidoRepository pedidos;
    @Autowired private ItemPedidoRepository itens;
    @Autowired private PontoFidelidadeRepository pontos;
    @Autowired private NotificacaoEnviadaRepository notificacoes;

    private Cliente novoCliente(LocalDate nascimento) {
        return Cliente.builder().nome("C-" + UUID.randomUUID()).optIn(true)
            .preferenciaCanal(PreferenciaCanal.EMAIL).dataNascimento(nascimento).build();
    }

    @Test
    void saldoPorCliente_somenteDaLojaAtual() {
        Long clienteId = TenantContext.calcular(1L, () -> {
            Cliente c = clientes.save(novoCliente(null));
            pontos.save(PontoFidelidade.builder().cliente(c).pontos(100).tipo(TipoPonto.CREDITO).build());
            return c.getId();
        });

        assertThat(TenantContext.calcular(1L, () -> pontos.saldoPorCliente(clienteId))).isEqualTo(100);
        assertThat(TenantContext.calcular(2L, () -> pontos.saldoPorCliente(clienteId))).isZero();
    }

    @Test
    void findAniversariantesHoje_somenteDaLojaAtual() {
        LocalDate hoje = LocalDate.now();
        Long id = TenantContext.calcular(1L, () -> clientes.save(novoCliente(hoje.minusYears(30))).getId());

        assertThat(TenantContext.calcular(1L, () ->
            clientes.findAniversariantesHoje(hoje.getMonthValue(), hoje.getDayOfMonth())))
            .anyMatch(c -> c.getId().equals(id));
        assertThat(TenantContext.calcular(2L, () ->
            clientes.findAniversariantesHoje(hoje.getMonthValue(), hoje.getDayOfMonth())))
            .noneMatch(c -> c.getId().equals(id));
    }

    @Test
    void jaEnviouAniversarioNoAno_somenteDaLojaAtual() {
        int ano = LocalDate.now().getYear();
        Long clienteId = TenantContext.calcular(1L, () -> {
            Cliente c = clientes.save(novoCliente(null));
            notificacoes.save(NotificacaoEnviada.builder().cliente(c)
                .evento(EventoNotificacao.ANIVERSARIO_CLIENTE).canal(CanalNotificacao.EMAIL)
                .destinatario("a@a.com").status(StatusEnvio.ENVIADA).build());
            return c.getId();
        });

        assertThat(TenantContext.calcular(1L, () ->
            notificacoes.jaEnviouAniversarioNoAno(clienteId, "ANIVERSARIO_CLIENTE", "EMAIL", ano))).isTrue();
        assertThat(TenantContext.calcular(2L, () ->
            notificacoes.jaEnviouAniversarioNoAno(clienteId, "ANIVERSARIO_CLIENTE", "EMAIL", ano))).isFalse();
    }

    @Test
    void consultasDeAnalytics_somenteDaLojaAtual() {
        int ano = LocalDate.now().getYear();
        TenantContext.executar(1L, () -> {
            Cliente c = clientes.save(novoCliente(null));
            Produto p = produtos.save(Produto.builder().nome("P-" + UUID.randomUUID())
                .categoria(Categoria.TRUFADO).precoVenda(BigDecimal.TEN).build());
            Pedido pedido = pedidos.save(Pedido.builder().cliente(c).status(StatusPedido.ENTREGUE)
                .totalPedido(new BigDecimal("100.00")).build());
            itens.save(ItemPedido.builder().pedido(pedido).produto(p).quantidade(2)
                .precoUnitario(new BigDecimal("50.00")).build());
        });

        assertThat(TenantContext.calcular(1L, () -> pedidos.countPorAno(ano))).isPositive();
        assertThat(TenantContext.calcular(1L, () -> pedidos.totalPorAno(ano))).isPositive();
        assertThat(TenantContext.calcular(1L, () -> pedidos.faturamentoPorMes(ano))).isNotEmpty();
        assertThat(TenantContext.calcular(1L, () -> pedidos.anosComPedidos())).contains(ano);
        assertThat(TenantContext.calcular(1L, () -> itens.rankingProdutosPorAno(ano))).isNotEmpty();

        assertThat(TenantContext.calcular(2L, () -> pedidos.countPorAno(ano))).isZero();
        assertThat(TenantContext.calcular(2L, () -> pedidos.totalPorAno(ano))).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(TenantContext.calcular(2L, () -> pedidos.faturamentoPorMes(ano))).isEmpty();
        assertThat(TenantContext.calcular(2L, () -> pedidos.anosComPedidos())).isEmpty();
        assertThat(TenantContext.calcular(2L, () -> itens.rankingProdutosPorAno(ano))).isEmpty();
    }
}
