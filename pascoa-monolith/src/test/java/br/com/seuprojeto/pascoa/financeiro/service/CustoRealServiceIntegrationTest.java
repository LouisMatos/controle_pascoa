package br.com.seuprojeto.pascoa.financeiro.service;

import br.com.seuprojeto.pascoa.cadastro.entity.Categoria;
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
import br.com.seuprojeto.pascoa.financeiro.dto.CustoRealDto;
import br.com.seuprojeto.pascoa.financeiro.entity.CategoriaDespesaVariavel;
import br.com.seuprojeto.pascoa.financeiro.entity.DespesaVariavel;
import br.com.seuprojeto.pascoa.financeiro.repository.DespesaFixaRepository;
import br.com.seuprojeto.pascoa.financeiro.repository.DespesaVariavelRepository;
import br.com.seuprojeto.pascoa.pedido.entity.ItemPedido;
import br.com.seuprojeto.pascoa.pedido.entity.Pedido;
import br.com.seuprojeto.pascoa.pedido.repository.PedidoRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes de integração para {@link CustoRealService}.
 * Usa banco H2 em memória (perfil "test") e rollback automático por teste.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CustoRealServiceIntegrationTest {

    @Autowired private CustoRealService custoRealService;

    @Autowired private PedidoRepository       pedidoRepository;
    @Autowired private ClienteRepository      clienteRepository;
    @Autowired private ProdutoRepository      produtoRepository;
    @Autowired private MateriaPrimaRepository materiaPrimaRepository;
    @Autowired private FichaTecnicaRepository fichaTecnicaRepository;
    @Autowired private DespesaVariavelRepository despesaVariavelRepository;
    @Autowired private DespesaFixaRepository  despesaFixaRepository;
    @Autowired private EntityManager          em;

    // ── helpers ──────────────────────────────────────────────────────────────

    private Cliente salvarCliente(String nome) {
        return clienteRepository.save(Cliente.builder()
                .nome(nome)
                .optIn(false)
                .preferenciaCanal(PreferenciaCanal.NENHUM)
                .build());
    }

    private Produto salvarProduto(String nome) {
        return produtoRepository.save(Produto.builder()
                .nome(nome)
                .categoria(Categoria.TRADICIONAL)
                .precoVenda(new BigDecimal("50.00"))
                .ativo(true)
                .build());
    }

    private MateriaPrima salvarMP(String nome, BigDecimal custoMedio, BigDecimal custoUnit) {
        return materiaPrimaRepository.save(MateriaPrima.builder()
                .nome(nome)
                .unidade(Unidade.KG)
                .quantidadeAtual(BigDecimal.TEN)
                .quantidadeMinima(BigDecimal.ZERO)
                .custoUnitario(custoUnit)
                .custoMedioPonderado(custoMedio)
                .build());
    }

    private FichaTecnica salvarFichaTecnica(Produto produto, BigDecimal rendimento) {
        return fichaTecnicaRepository.save(FichaTecnica.builder()
                .produto(produto)
                .rendimento(rendimento)
                .unidadeRendimento(Unidade.UN)
                .build());
    }

    /**
     * Cria e persiste um pedido simples com um único item.
     */
    private Pedido salvarPedido(Cliente cliente, Produto produto,
                                int qtd, BigDecimal precoUnit) {
        Pedido pedido = Pedido.builder()
                .cliente(cliente)
                .totalPedido(precoUnit.multiply(BigDecimal.valueOf(qtd)))
                .build();
        ItemPedido item = ItemPedido.builder()
                .pedido(pedido)
                .produto(produto)
                .quantidade(qtd)
                .precoUnitario(precoUnit)
                .build();
        pedido.getItens().add(item);
        return pedidoRepository.save(pedido);
    }

    /** Limpa o contexto de persistência para forçar recarga do banco. */
    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    // ── testes ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Pedido inexistente → lança IllegalArgumentException com o ID na mensagem")
    void calcular_pedidoNaoEncontrado_lancaExcecao() {
        assertThatThrownBy(() -> custoRealService.calcular(99999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99999");
    }

    @Test
    @DisplayName("Produto sem ficha técnica → custoMP = 0, linha retornada com nome e quantidade corretos")
    void calcular_semFichaTecnica_custoMPZero() {
        Produto produto = salvarProduto("Ovo Tradicional Puro");
        Cliente cliente = salvarCliente("Maria");
        Pedido pedido   = salvarPedido(cliente, produto, 2, new BigDecimal("50.00"));
        flushAndClear();

        CustoRealDto dto = custoRealService.calcular(pedido.getId());

        assertThat(dto.getCustoMP()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.getLinhasMP()).hasSize(1);
        assertThat(dto.getLinhasMP().get(0).getProduto()).isEqualTo("Ovo Tradicional Puro");
        assertThat(dto.getLinhasMP().get(0).getQuantidade()).isEqualTo(2);
        assertThat(dto.getLinhasMP().get(0).getSubtotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.getClienteNome()).isEqualTo("Maria");
        assertThat(dto.getTotalPedido()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("Produto com ficha técnica usa custoMedioPonderado para calcular custo MP")
    void calcular_comFichaTecnicaECMP_calculaCustoMPCorretamente() {
        // Arrange
        Produto produto = salvarProduto("Ovo Trufado");
        Cliente cliente = salvarCliente("João");

        // MP: custo médio ponderado = R$ 20,00/kg  |  custo unitário = R$ 15,00/kg (não deve ser usado)
        MateriaPrima mp = salvarMP("Chocolate 70%", new BigDecimal("20.00"), new BigDecimal("15.00"));

        // Ficha técnica: rendimento = 1 un, usa 0,5 kg de MP por unidade produzida
        FichaTecnica ft = salvarFichaTecnica(produto, BigDecimal.ONE);
        ft.getItens().add(FichaTecnicaItem.builder()
                .fichaTecnica(ft)
                .materiaPrima(mp)
                .quantidade(new BigDecimal("0.500"))
                .build());
        fichaTecnicaRepository.save(ft);

        // Pedido: 3 unidades × R$ 60,00
        Pedido pedido = salvarPedido(cliente, produto, 3, new BigDecimal("60.00"));
        flushAndClear();

        // Act
        CustoRealDto dto = custoRealService.calcular(pedido.getId());

        // custo unitário = (0,5 kg × R$ 20,00) / 1 = R$ 10,00
        // subtotal       = R$ 10,00 × 3 = R$ 30,00
        assertThat(dto.getLinhasMP()).hasSize(1);
        assertThat(dto.getLinhasMP().get(0).getCustoUnitario())
                .isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(dto.getLinhasMP().get(0).getSubtotal())
                .isEqualByComparingTo(new BigDecimal("30.00"));
        assertThat(dto.getCustoMP()).isEqualByComparingTo(new BigDecimal("30.00"));
    }

    @Test
    @DisplayName("custoMedioPonderado == 0 → usa custoUnitario como fallback")
    void calcular_custoMedioPonderadoZero_usaCustoUnitarioComoFallback() {
        Produto produto = salvarProduto("Ovo Recheado");
        Cliente cliente = salvarCliente("Ana");

        // custo médio = 0  →  deve usar custo unitário = R$ 8,00
        MateriaPrima mp = salvarMP("Recheio de Morango", BigDecimal.ZERO, new BigDecimal("8.00"));

        // Ficha técnica: rendimento = 2 un, usa 0,25 kg de MP por lote
        FichaTecnica ft = salvarFichaTecnica(produto, new BigDecimal("2.000"));
        ft.getItens().add(FichaTecnicaItem.builder()
                .fichaTecnica(ft)
                .materiaPrima(mp)
                .quantidade(new BigDecimal("0.250"))
                .build());
        fichaTecnicaRepository.save(ft);

        Pedido pedido = salvarPedido(cliente, produto, 1, new BigDecimal("30.00"));
        flushAndClear();

        CustoRealDto dto = custoRealService.calcular(pedido.getId());

        // custo unitário = (0,25 kg × R$ 8,00) / 2 = R$ 1,00
        assertThat(dto.getLinhasMP().get(0).getCustoUnitario())
                .isEqualByComparingTo(new BigDecimal("1.00"));
        assertThat(dto.getCustoMP()).isEqualByComparingTo(new BigDecimal("1.00"));
    }

    @Test
    @DisplayName("Despesas variáveis são somadas corretamente ao custo real")
    void calcular_comDespesasVariaveis_somadasAoCustoReal() {
        Produto produto = salvarProduto("Ovo Diet");
        Cliente cliente = salvarCliente("Pedro");
        Pedido pedido   = salvarPedido(cliente, produto, 1, new BigDecimal("80.00"));

        despesaVariavelRepository.save(DespesaVariavel.builder()
                .pedido(pedido)
                .descricao("Embalagem premium")
                .valor(new BigDecimal("12.50"))
                .categoria(CategoriaDespesaVariavel.EMBALAGEM)
                .build());

        flushAndClear();

        CustoRealDto dto = custoRealService.calcular(pedido.getId());

        // custoMP = 0 (sem FT), rateioFixo = 0 (sem despesas fixas), variavel = 12,50
        assertThat(dto.getDespesasVariaveis()).isEqualByComparingTo(new BigDecimal("12.50"));
        assertThat(dto.getCustoReal()).isEqualByComparingTo(new BigDecimal("12.50"));
        assertThat(dto.getDespesasDetalhadas()).hasSize(1);
        assertThat(dto.getDespesasDetalhadas().get(0).getDescricao())
                .isEqualTo("Embalagem premium");
    }

    @Test
    @DisplayName("Sem despesas fixas cadastradas → rateioFixo = 0")
    void calcular_semDespesasFixas_rateioFixoZero() {
        Produto produto = salvarProduto("Ovo Vegano");
        Cliente cliente = salvarCliente("Laura");
        Pedido pedido   = salvarPedido(cliente, produto, 2, new BigDecimal("45.00"));
        flushAndClear();

        CustoRealDto dto = custoRealService.calcular(pedido.getId());

        assertThat(dto.getRateioFixo()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("totalPedido = 0 → margemReal = 0 (sem divisão por zero)")
    void calcular_totalPedidoZero_margemRealZeroSemExcecao() {
        Produto produto = salvarProduto("Ovo Brinde");
        Cliente cliente = salvarCliente("Carla");
        Pedido pedido   = salvarPedido(cliente, produto, 1, BigDecimal.ZERO);
        flushAndClear();

        CustoRealDto dto = custoRealService.calcular(pedido.getId());

        assertThat(dto.getMargemReal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.getTotalPedido()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Custo real e margem calculados corretamente com todos os componentes")
    void calcular_totalEMargemCalculadosComCustoMpEVariavel() {
        Produto produto = salvarProduto("Ovo Especial");
        Cliente cliente = salvarCliente("Roberto");

        MateriaPrima mp = salvarMP("Cacau puro", new BigDecimal("10.00"), new BigDecimal("10.00"));
        FichaTecnica ft = salvarFichaTecnica(produto, BigDecimal.ONE);
        ft.getItens().add(FichaTecnicaItem.builder()
                .fichaTecnica(ft)
                .materiaPrima(mp)
                .quantidade(new BigDecimal("1.000"))
                .build());
        fichaTecnicaRepository.save(ft);

        // Pedido: 2 unidades × R$ 50,00 = R$ 100,00 total
        Pedido pedido = salvarPedido(cliente, produto, 2, new BigDecimal("50.00"));

        // Despesa variável de frete: R$ 5,00
        despesaVariavelRepository.save(DespesaVariavel.builder()
                .pedido(pedido)
                .descricao("Frete")
                .valor(new BigDecimal("5.00"))
                .categoria(CategoriaDespesaVariavel.FRETE)
                .build());

        flushAndClear();

        CustoRealDto dto = custoRealService.calcular(pedido.getId());

        // custoMP = (1 kg × R$ 10) / 1 × 2 un = R$ 20,00
        // variavel = R$ 5,00
        // custo real = R$ 20,00 + R$ 0,00 (rateio) + R$ 5,00 = R$ 25,00
        // margem = (100 - 25) / 100 × 100 = 75,0%
        assertThat(dto.getCustoMP()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(dto.getDespesasVariaveis()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(dto.getCustoReal()).isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(dto.getMargemReal()).isEqualByComparingTo(new BigDecimal("75.0"));
        assertThat(dto.getPedidoId()).isEqualTo(pedido.getId());
    }
}
