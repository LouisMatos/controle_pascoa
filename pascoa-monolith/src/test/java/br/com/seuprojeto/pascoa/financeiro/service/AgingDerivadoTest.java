package br.com.seuprojeto.pascoa.financeiro.service;

import br.com.seuprojeto.pascoa.cadastro.entity.Cliente;
import br.com.seuprojeto.pascoa.cadastro.entity.PreferenciaCanal;
import br.com.seuprojeto.pascoa.cadastro.entity.Produto;
import br.com.seuprojeto.pascoa.cadastro.repository.ClienteRepository;
import br.com.seuprojeto.pascoa.cadastro.repository.ProdutoRepository;
import br.com.seuprojeto.pascoa.pedido.dto.PagamentoForm;
import br.com.seuprojeto.pascoa.pedido.entity.Pedido;
import br.com.seuprojeto.pascoa.pedido.entity.TipoPagamento;
import br.com.seuprojeto.pascoa.pedido.repository.PedidoRepository;
import br.com.seuprojeto.pascoa.pedido.service.PedidoService;
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

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@WithMockUser(roles = "ADMIN")
class AgingDerivadoTest {

    @Autowired private BreakevenService breakevenService;
    @Autowired private PedidoService pedidoService;
    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private ProdutoRepository produtoRepository;
    @Autowired private EntityManager em;

    private Cliente cliente;
    private Produto produto;

    @BeforeEach
    void setUp() {
        cliente = clienteRepository.save(Cliente.builder()
                .nome("Cliente Aging")
                .optIn(false)
                .preferenciaCanal(PreferenciaCanal.NENHUM)
                .build());
        produto = produtoRepository.save(Produto.builder()
                .nome("Ovo Aging")
                .precoVenda(new BigDecimal("50.00"))
                .ativo(true)
                .build());
        em.flush();
        em.clear();
    }

    private Long pedidoConfirmado(LocalDate dataEntrega) {
        // o serviço recusa entrega no passado; pedido vencido é criado no futuro e recuado direto na entidade
        LocalDate entregaValida = dataEntrega.isBefore(LocalDate.now()) ? LocalDate.now().plusDays(1) : dataEntrega;
        Pedido pedido = pedidoService.criarComItens(
                cliente.getId(), entregaValida, null, null, List.of(produto.getId()), List.of(2));
        em.flush();
        em.clear();
        pedidoService.confirmar(pedido.getId());
        em.flush();
        em.clear();
        if (!entregaValida.equals(dataEntrega)) {
            Pedido vencido = pedidoRepository.findById(pedido.getId()).orElseThrow();
            vencido.setDataEntrega(dataEntrega);
            pedidoRepository.save(vencido);
            em.flush();
            em.clear();
        }
        return pedido.getId();
    }

    private void pagar(Long pedidoId, String valor) {
        PagamentoForm form = new PagamentoForm();
        form.setValor(new BigDecimal(valor));
        form.setTipoPagamento(TipoPagamento.PIX);
        form.setDataPagamento(LocalDate.now());
        pedidoService.registrarPagamento(pedidoId, form);
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("Pedido com pagamento parcial entra no aging com o saldo restante")
    void pagamentoParcial_apareceComSaldoRestante() {
        Long pedidoId = pedidoConfirmado(LocalDate.now().plusDays(5));
        pagar(pedidoId, "40.00");

        var aging = breakevenService.aging();

        assertThat(aging.getTotalGeral()).isEqualByComparingTo(new BigDecimal("60.00"));
        assertThat(aging.getCorrente())
                .anySatisfy(l -> assertThat(l.getPedidoId()).isEqualTo(pedidoId));
    }

    @Test
    @DisplayName("Pedido quitado não aparece no aging")
    void pedidoQuitado_naoAparece() {
        Long pedidoId = pedidoConfirmado(LocalDate.now().plusDays(5));
        pagar(pedidoId, "100.00");

        var aging = breakevenService.aging();

        assertThat(aging.getTotalGeral()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Pedido cancelado não aparece no aging")
    void pedidoCancelado_naoAparece() {
        Long pedidoId = pedidoConfirmado(LocalDate.now().plusDays(5));
        pedidoService.cancelar(pedidoId);
        em.flush();
        em.clear();

        var aging = breakevenService.aging();

        assertThat(aging.getTotalGeral()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Vencimento há 45 dias cai na faixa de 31 a 60 dias de atraso")
    void vencidoHa45Dias_caiNaFaixaCorreta() {
        Long pedidoId = pedidoConfirmado(LocalDate.now().minusDays(45));

        var aging = breakevenService.aging();

        assertThat(aging.getTotal31a60()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(aging.getAtraso31a60())
                .anySatisfy(l -> assertThat(l.getPedidoId()).isEqualTo(pedidoId));
    }

    @Test
    @DisplayName("Saldo em aberto no período alimenta o previsto de entrada do fluxo de caixa")
    void saldoEmAberto_porVencimento() {
        pedidoConfirmado(LocalDate.now().plusDays(3));

        BigDecimal previsto = pedidoRepository.sumSaldoEmAbertoPorVencimento(
                LocalDate.now(), LocalDate.now().plusDays(10));

        assertThat(previsto).isEqualByComparingTo(new BigDecimal("100.00"));
    }
}
