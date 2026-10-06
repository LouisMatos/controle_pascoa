package br.com.seuprojeto.pascoa.producao.service;

import br.com.seuprojeto.pascoa.cadastro.entity.Cliente;
import br.com.seuprojeto.pascoa.cadastro.entity.MateriaPrima;
import br.com.seuprojeto.pascoa.cadastro.entity.PreferenciaCanal;
import br.com.seuprojeto.pascoa.cadastro.entity.Produto;
import br.com.seuprojeto.pascoa.cadastro.entity.Unidade;
import br.com.seuprojeto.pascoa.cadastro.repository.ClienteRepository;
import br.com.seuprojeto.pascoa.cadastro.repository.MateriaPrimaRepository;
import br.com.seuprojeto.pascoa.cadastro.repository.ProdutoRepository;
import br.com.seuprojeto.pascoa.fichaTecnica.entity.FichaTecnica;
import br.com.seuprojeto.pascoa.fichaTecnica.entity.FichaTecnicaItem;
import br.com.seuprojeto.pascoa.fichaTecnica.repository.FichaTecnicaRepository;
import br.com.seuprojeto.pascoa.pedido.entity.Pedido;
import br.com.seuprojeto.pascoa.pedido.entity.StatusPedido;
import br.com.seuprojeto.pascoa.pedido.repository.PedidoRepository;
import br.com.seuprojeto.pascoa.pedido.service.PedidoService;
import br.com.seuprojeto.pascoa.producao.entity.OrdemProducao;
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
class ProducaoStatusPedidoTest {

    @Autowired private PedidoService pedidoService;
    @Autowired private ProducaoService producaoService;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private ProdutoRepository produtoRepository;
    @Autowired private MateriaPrimaRepository materiaPrimaRepository;
    @Autowired private FichaTecnicaRepository fichaTecnicaRepository;
    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private EntityManager em;

    private Cliente cliente;
    private Produto produtoA;
    private Produto produtoB;

    @BeforeEach
    void setUp() {
        cliente = clienteRepository.save(Cliente.builder()
                .nome("Cliente Producao")
                .optIn(false)
                .preferenciaCanal(PreferenciaCanal.NENHUM)
                .build());

        MateriaPrima chocolate = materiaPrimaRepository.save(MateriaPrima.builder()
                .nome("Chocolate Producao")
                .unidade(Unidade.KG)
                .quantidadeAtual(new BigDecimal("100.000"))
                .quantidadeMinima(BigDecimal.ONE)
                .custoUnitario(new BigDecimal("40.00"))
                .build());

        produtoA = produtoComFicha("Ovo Producao A", chocolate);
        produtoB = produtoComFicha("Ovo Producao B", chocolate);
        em.flush();
        em.clear();
    }

    private Produto produtoComFicha(String nome, MateriaPrima materiaPrima) {
        Produto produto = produtoRepository.save(Produto.builder()
                .nome(nome)
                .precoVenda(new BigDecimal("50.00"))
                .ativo(true)
                .build());

        FichaTecnica ficha = FichaTecnica.builder()
                .produto(produto)
                .rendimento(BigDecimal.ONE)
                .build();
        ficha.getItens().add(FichaTecnicaItem.builder()
                .fichaTecnica(ficha)
                .materiaPrima(materiaPrima)
                .quantidade(new BigDecimal("0.500"))
                .build());
        fichaTecnicaRepository.save(ficha);
        return produto;
    }

    private Long pedidoConfirmado(List<Long> produtoIds, List<Integer> quantidades) {
        Pedido pedido = pedidoService.criarComItens(
                cliente.getId(), LocalDate.now().plusDays(3), null, null, produtoIds, quantidades);
        em.flush();
        em.clear();
        pedidoService.confirmar(pedido.getId());
        em.flush();
        em.clear();
        return pedido.getId();
    }

    private StatusPedido statusAtual(Long pedidoId) {
        em.flush();
        em.clear();
        return pedidoRepository.findById(pedidoId).orElseThrow().getStatus();
    }

    @Test
    @DisplayName("Iniciar ordem leva pedido CONFIRMADO para EM_PRODUCAO")
    void iniciarOrdem_pedidoVaiParaEmProducao() {
        Long pedidoId = pedidoConfirmado(List.of(produtoA.getId()), List.of(1));
        OrdemProducao ordem = producaoService.listarPorPedido(pedidoId).get(0);

        producaoService.iniciarProducao(ordem.getId());

        assertThat(statusAtual(pedidoId)).isEqualTo(StatusPedido.EM_PRODUCAO);
    }

    @Test
    @DisplayName("Concluir a última ordem aberta leva pedido para PRONTO")
    void concluirUltimaOrdem_pedidoVaiParaPronto() {
        Long pedidoId = pedidoConfirmado(List.of(produtoA.getId()), List.of(1));
        OrdemProducao ordem = producaoService.listarPorPedido(pedidoId).get(0);

        producaoService.concluirOrdem(ordem.getId());

        assertThat(statusAtual(pedidoId)).isEqualTo(StatusPedido.PRONTO);
    }

    @Test
    @DisplayName("Concluir uma ordem com outra pendente não leva pedido para PRONTO")
    void concluirUmaOrdem_comOutraPendente_pedidoNaoVaiParaPronto() {
        Long pedidoId = pedidoConfirmado(
                List.of(produtoA.getId(), produtoB.getId()), List.of(1, 1));
        List<OrdemProducao> ordens = producaoService.listarPorPedido(pedidoId);
        assertThat(ordens).hasSize(2);

        producaoService.concluirOrdem(ordens.get(0).getId());

        assertThat(statusAtual(pedidoId)).isEqualTo(StatusPedido.CONFIRMADO);
    }

    @Test
    @DisplayName("Cancelar a única ordem pendente não leva pedido para PRONTO")
    void cancelarOrdem_pedidoNaoVaiParaPronto() {
        Long pedidoId = pedidoConfirmado(List.of(produtoA.getId()), List.of(1));
        OrdemProducao ordem = producaoService.listarPorPedido(pedidoId).get(0);

        producaoService.cancelarOrdem(ordem.getId());

        assertThat(statusAtual(pedidoId)).isEqualTo(StatusPedido.CONFIRMADO);
    }
}
