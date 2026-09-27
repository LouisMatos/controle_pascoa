package br.com.seuprojeto.pascoa.crm.service;

import br.com.seuprojeto.pascoa.cadastro.entity.Cliente;
import br.com.seuprojeto.pascoa.cadastro.entity.PreferenciaCanal;
import br.com.seuprojeto.pascoa.cadastro.repository.ClienteRepository;
import br.com.seuprojeto.pascoa.crm.entity.PontoFidelidade;
import br.com.seuprojeto.pascoa.crm.entity.SegmentoCliente;
import br.com.seuprojeto.pascoa.crm.entity.TipoPonto;
import br.com.seuprojeto.pascoa.crm.repository.PontoFidelidadeRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de integração para segmentação de clientes e pontos de fidelidade.
 *
 * <p>Cobre:
 * <ul>
 *   <li>F9 — {@code saldoPorCliente()} exclui créditos expirados</li>
 *   <li>F8 — {@code recalcularSegmentos()} persiste segmento na entidade Cliente</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CrmSegmentoTest {

    @Autowired private CrmService              crmService;
    @Autowired private ClienteRepository       clienteRepository;
    @Autowired private PontoFidelidadeRepository pontoRepository;
    @Autowired private EntityManager           em;

    private Cliente cliente;

    @BeforeEach
    void setUp() {
        cliente = clienteRepository.save(Cliente.builder()
                .nome("Cliente Pontos")
                .optIn(false)
                .preferenciaCanal(PreferenciaCanal.NENHUM)
                .build());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // F9 — saldoPorCliente respeita data de expiração
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("F9: Créditos não-expirados contam no saldo")
    void saldo_creditosValidos_somados() {
        pontoRepository.save(PontoFidelidade.builder()
                .cliente(cliente).pontos(100).tipo(TipoPonto.CREDITO)
                .descricao("Compra").dataExpiracao(LocalDate.now().plusDays(30)).build());
        pontoRepository.save(PontoFidelidade.builder()
                .cliente(cliente).pontos(50).tipo(TipoPonto.CREDITO)
                .descricao("Boas-vindas").dataExpiracao(null).build()); // sem expiração
        em.flush(); em.clear();

        int saldo = pontoRepository.saldoPorCliente(cliente.getId());

        assertThat(saldo).isEqualTo(150);
    }

    @Test
    @DisplayName("F9: Créditos expirados NÃO contam no saldo")
    void saldo_creditosExpirados_ignorados() {
        // Crédito válido
        pontoRepository.save(PontoFidelidade.builder()
                .cliente(cliente).pontos(200).tipo(TipoPonto.CREDITO)
                .descricao("Válido").dataExpiracao(LocalDate.now().plusDays(1)).build());
        // Crédito expirado (ontem)
        pontoRepository.save(PontoFidelidade.builder()
                .cliente(cliente).pontos(1000).tipo(TipoPonto.CREDITO)
                .descricao("Expirado").dataExpiracao(LocalDate.now().minusDays(1)).build());
        em.flush(); em.clear();

        int saldo = pontoRepository.saldoPorCliente(cliente.getId());

        assertThat(saldo).isEqualTo(200); // os 1000 expirados são ignorados
    }

    @Test
    @DisplayName("F9: Débitos são sempre deduzidos, independente de expiração")
    void saldo_debitosDeduzemSempre() {
        pontoRepository.save(PontoFidelidade.builder()
                .cliente(cliente).pontos(500).tipo(TipoPonto.CREDITO)
                .descricao("Saldo inicial").dataExpiracao(null).build());
        pontoRepository.save(PontoFidelidade.builder()
                .cliente(cliente).pontos(150).tipo(TipoPonto.DEBITO)
                .descricao("Resgate").build());
        em.flush(); em.clear();

        int saldo = pontoRepository.saldoPorCliente(cliente.getId());

        assertThat(saldo).isEqualTo(350);
    }

    @Test
    @DisplayName("F9: Crédito expirado exatamente hoje ainda conta (data_expiracao = CURRENT_DATE)")
    void saldo_creditoExpiraHoje_aindaValido() {
        pontoRepository.save(PontoFidelidade.builder()
                .cliente(cliente).pontos(300).tipo(TipoPonto.CREDITO)
                .descricao("Expira hoje").dataExpiracao(LocalDate.now()).build());
        em.flush(); em.clear();

        int saldo = pontoRepository.saldoPorCliente(cliente.getId());

        assertThat(saldo).isEqualTo(300); // >= CURRENT_DATE inclui o dia de hoje
    }

    @Test
    @DisplayName("F9: Cliente sem pontos retorna saldo zero")
    void saldo_semPontos_retornaZero() {
        em.flush(); em.clear();

        int saldo = pontoRepository.saldoPorCliente(cliente.getId());

        assertThat(saldo).isEqualTo(0);
    }

    @Test
    @DisplayName("F9: Todos créditos expirados → saldo é zero (não negativo)")
    void saldo_todosExpirados_saldoZero() {
        pontoRepository.save(PontoFidelidade.builder()
                .cliente(cliente).pontos(500).tipo(TipoPonto.CREDITO)
                .descricao("Antigo").dataExpiracao(LocalDate.now().minusYears(1)).build());
        em.flush(); em.clear();

        int saldo = pontoRepository.saldoPorCliente(cliente.getId());

        // Expirados são ignorados → CASE retorna 0 para eles → soma = 0
        assertThat(saldo).isEqualTo(0);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // F8 — recalcularSegmentos persiste segmento
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("F8: Cliente sem pedidos recebe segmento NOVO após recalcularSegmentos()")
    void recalcularSegmentos_semPedidos_segmentoNovo() {
        // cliente criado no @BeforeEach — sem pedidos
        em.flush(); em.clear();

        crmService.recalcularSegmentos();
        em.flush(); em.clear();

        Cliente atualizado = clienteRepository.findById(cliente.getId()).orElseThrow();
        assertThat(atualizado.getSegmento()).isEqualTo(SegmentoCliente.NOVO);
    }

    @Test
    @DisplayName("F8: Dois clientes distintos recebem segmentos independentes")
    void recalcularSegmentos_variosClientes_atualizadosIndependentemente() {
        Cliente clienteB = clienteRepository.save(Cliente.builder()
                .nome("Cliente B sem pedidos")
                .optIn(false)
                .preferenciaCanal(PreferenciaCanal.NENHUM)
                .build());
        em.flush(); em.clear();

        crmService.recalcularSegmentos();
        em.flush(); em.clear();

        // Ambos sem pedidos → ambos NOVO
        assertThat(clienteRepository.findById(cliente.getId()).orElseThrow().getSegmento())
                .isEqualTo(SegmentoCliente.NOVO);
        assertThat(clienteRepository.findById(clienteB.getId()).orElseThrow().getSegmento())
                .isEqualTo(SegmentoCliente.NOVO);
    }
}
