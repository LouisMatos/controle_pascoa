package br.com.seuprojeto.pascoa.orcamento.service;

import br.com.seuprojeto.pascoa.cadastro.entity.Categoria;
import br.com.seuprojeto.pascoa.cadastro.entity.Cliente;
import br.com.seuprojeto.pascoa.cadastro.entity.PreferenciaCanal;
import br.com.seuprojeto.pascoa.cadastro.entity.Produto;
import br.com.seuprojeto.pascoa.cadastro.repository.ClienteRepository;
import br.com.seuprojeto.pascoa.cadastro.repository.ProdutoRepository;
import br.com.seuprojeto.pascoa.orcamento.dto.OrcamentoForm;
import br.com.seuprojeto.pascoa.orcamento.dto.OrcamentoItemForm;
import br.com.seuprojeto.pascoa.orcamento.entity.Orcamento;
import br.com.seuprojeto.pascoa.orcamento.entity.StatusOrcamento;
import br.com.seuprojeto.pascoa.pedido.entity.Pedido;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes de integração para {@link OrcamentoService}.
 * Cobre o fluxo completo: criação → aprovação/recusa → conversão em pedido.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrcamentoServiceIntegrationTest {

    @Autowired private OrcamentoService    service;
    @Autowired private ClienteRepository  clienteRepository;
    @Autowired private ProdutoRepository  produtoRepository;
    @Autowired private EntityManager      em;

    private Cliente cliente;
    private Produto produto;

    @BeforeEach
    void setUp() {
        cliente = clienteRepository.save(Cliente.builder()
                .nome("Cliente Teste")
                .optIn(false)
                .preferenciaCanal(PreferenciaCanal.NENHUM)
                .build());

        produto = produtoRepository.save(Produto.builder()
                .nome("Ovo de Páscoa Trufado 500g")
                .categoria(Categoria.TRUFADO)
                .precoVenda(new BigDecimal("89.90"))
                .ativo(true)
                .build());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private OrcamentoForm formComUmItem(int qtd, BigDecimal preco) {
        OrcamentoItemForm itemForm = new OrcamentoItemForm();
        itemForm.setProdutoId(produto.getId());
        itemForm.setQuantidade(qtd);
        itemForm.setPrecoUnitario(preco);

        OrcamentoForm form = new OrcamentoForm();
        form.setClienteId(cliente.getId());
        form.setValidade(LocalDate.now().plusDays(7));
        form.setObservacoes("Teste de integração");
        form.setItens(List.of(itemForm));
        return form;
    }

    // ── testes ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Criar orçamento gera token único e persiste itens com total correto")
    void criar_geraTokenEPersiste() {
        Orcamento orc = service.criar(formComUmItem(2, new BigDecimal("89.90")), "operador");

        assertThat(orc.getId()).isNotNull();
        assertThat(orc.getTokenAprovacao()).isNotBlank();
        assertThat(orc.getStatus()).isEqualTo(StatusOrcamento.PENDENTE);
        assertThat(orc.getItens()).hasSize(1);
        assertThat(orc.getTotal()).isEqualByComparingTo(new BigDecimal("179.80"));
        assertThat(orc.getCriadoPor()).isEqualTo("operador");
    }

    @Test
    @DisplayName("buscarPorToken retorna orçamento correspondente ao token")
    void buscarPorToken_retornaOrcamento() {
        Orcamento criado = service.criar(formComUmItem(1, new BigDecimal("89.90")), "op");

        Orcamento encontrado = service.buscarPorToken(criado.getTokenAprovacao());

        assertThat(encontrado.getId()).isEqualTo(criado.getId());
        assertThat(encontrado.getCliente().getNome()).isEqualTo("Cliente Teste");
    }

    @Test
    @DisplayName("buscarPorToken com token inválido lança EntityNotFoundException")
    void buscarPorToken_tokenInvalido_lancaExcecao() {
        assertThatThrownBy(() -> service.buscarPorToken("token-invalido-xyz"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("Aprovar orçamento PENDENTE muda status para APROVADO")
    void aprovar_pendenteValido_mudaStatusParaAprovado() {
        Orcamento orc = service.criar(formComUmItem(1, new BigDecimal("89.90")), "op");

        service.aprovar(orc.getTokenAprovacao());
        Orcamento atualizado = service.buscarPorToken(orc.getTokenAprovacao());

        assertThat(atualizado.getStatus()).isEqualTo(StatusOrcamento.APROVADO);
    }

    @Test
    @DisplayName("Recusar orçamento PENDENTE muda status para RECUSADO")
    void recusar_pendenteValido_mudaStatusParaRecusado() {
        Orcamento orc = service.criar(formComUmItem(1, new BigDecimal("89.90")), "op");

        service.recusar(orc.getTokenAprovacao());
        Orcamento atualizado = service.buscarPorToken(orc.getTokenAprovacao());

        assertThat(atualizado.getStatus()).isEqualTo(StatusOrcamento.RECUSADO);
    }

    @Test
    @DisplayName("Aprovar orçamento já RECUSADO lança IllegalStateException")
    void aprovar_jaRecusado_lancaExcecao() {
        Orcamento orc = service.criar(formComUmItem(1, new BigDecimal("89.90")), "op");
        service.recusar(orc.getTokenAprovacao());

        assertThatThrownBy(() -> service.aprovar(orc.getTokenAprovacao()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Converter orçamento APROVADO gera pedido com itens e total corretos")
    void converter_aprovado_geraPedidoComItensCorretos() {
        Orcamento orc = service.criar(formComUmItem(3, new BigDecimal("89.90")), "op");
        service.aprovar(orc.getTokenAprovacao());

        Pedido pedido = service.converter(orc.getId());

        // O converter() salva os ItemPedido via itemPedidoRepo separadamente,
        // sem adicioná-los à coleção em memória do Pedido (@OneToMany mappedBy).
        // O refresh força o reload do banco dentro da mesma transação.
        em.flush();
        em.refresh(pedido);

        assertThat(pedido.getId()).isNotNull();
        assertThat(pedido.getCliente().getId()).isEqualTo(cliente.getId());
        assertThat(pedido.getItens()).hasSize(1);
        assertThat(pedido.getTotalPedido()).isEqualByComparingTo(new BigDecimal("269.70"));
    }

    @Test
    @DisplayName("Converter orçamento não-APROVADO lança IllegalStateException")
    void converter_naoAprovado_lancaExcecao() {
        Orcamento orc = service.criar(formComUmItem(1, new BigDecimal("89.90")), "op");
        // ainda PENDENTE

        assertThatThrownBy(() -> service.converter(orc.getId()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Dois orçamentos geram tokens distintos")
    void criar_doisOrcamentos_tokensDistintos() {
        Orcamento orc1 = service.criar(formComUmItem(1, new BigDecimal("89.90")), "op");
        Orcamento orc2 = service.criar(formComUmItem(1, new BigDecimal("89.90")), "op");

        assertThat(orc1.getTokenAprovacao()).isNotEqualTo(orc2.getTokenAprovacao());
    }
}
