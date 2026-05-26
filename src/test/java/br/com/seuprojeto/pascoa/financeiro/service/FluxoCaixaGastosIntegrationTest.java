package br.com.seuprojeto.pascoa.financeiro.service;

import br.com.seuprojeto.pascoa.financeiro.dto.FluxoCaixaDto;
import br.com.seuprojeto.pascoa.gastos.entity.CategoriaGasto;
import br.com.seuprojeto.pascoa.gastos.entity.GastoVariavel;
import br.com.seuprojeto.pascoa.gastos.repository.GastoVariavelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes de integração que verificam a integração de {@link GastoVariavel}
 * no cálculo de {@link FluxoCaixaDto#getSaidaGastosVariaveis()}.
 *
 * <p>Confirma que:
 * <ul>
 *   <li>Gastos dentro do período são somados em saidaGastosVariaveis.</li>
 *   <li>Gastos fora do período não contaminam o resultado.</li>
 *   <li>Com zero gastos no período, o campo é zero (não null).</li>
 *   <li>O saldo realizado reflete os gastos do período.</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FluxoCaixaGastosIntegrationTest {

    @Autowired private FluxoCaixaService   fluxoCaixaService;
    @Autowired private GastoVariavelRepository gastoRepository;

    /** Período fixo usado nos testes: 01/04 a 30/04 do ano corrente. */
    private static final LocalDate INICIO = LocalDate.now().withMonth(4).withDayOfMonth(1);
    private static final LocalDate FIM    = LocalDate.now().withMonth(4).withDayOfMonth(30);

    @BeforeEach
    void limpar() {
        gastoRepository.deleteAll();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private GastoVariavel salvarGasto(BigDecimal valor, LocalDate data) {
        return gastoRepository.save(GastoVariavel.builder()
                .descricao("Gasto teste")
                .valor(valor)
                .dataLancamento(data)
                .categoria(CategoriaGasto.OUTROS)
                .referenciaMes(data.getMonthValue())
                .referenciaAno(data.getYear())
                .build());
    }

    // ── testes ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Gastos dentro do período somam em saidaGastosVariaveis")
    void gastosDentroDoPeriodo_somamEmSaidaGastos() {
        salvarGasto(new BigDecimal("100.00"), INICIO);
        salvarGasto(new BigDecimal("250.50"), INICIO.plusDays(10));
        salvarGasto(new BigDecimal("49.50"),  FIM);

        FluxoCaixaDto fluxo = fluxoCaixaService.calcular(INICIO, FIM);

        assertThat(fluxo.getSaidaGastosVariaveis())
                .isEqualByComparingTo(new BigDecimal("400.00"));
    }

    @Test
    @DisplayName("Gastos fora do período não afetam saidaGastosVariaveis")
    void gastosFoaDoPeriodo_naoContaminamResultado() {
        // Dentro do período
        salvarGasto(new BigDecimal("200.00"), INICIO.plusDays(5));
        // Fora do período (dia anterior e dia posterior)
        salvarGasto(new BigDecimal("999.99"), INICIO.minusDays(1));
        salvarGasto(new BigDecimal("999.99"), FIM.plusDays(1));

        FluxoCaixaDto fluxo = fluxoCaixaService.calcular(INICIO, FIM);

        assertThat(fluxo.getSaidaGastosVariaveis())
                .isEqualByComparingTo(new BigDecimal("200.00"));
    }

    @Test
    @DisplayName("Sem gastos no período, saidaGastosVariaveis é zero (não null)")
    void semGastosNoPeriodo_retornaZero() {
        // Apenas gastos fora do período
        salvarGasto(new BigDecimal("500.00"), INICIO.minusDays(30));

        FluxoCaixaDto fluxo = fluxoCaixaService.calcular(INICIO, FIM);

        assertThat(fluxo.getSaidaGastosVariaveis()).isNotNull();
        assertThat(fluxo.getSaidaGastosVariaveis())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Gasto no limite exato do período (primeiro e último dia) é incluído")
    void gastoNosLimitesExatos_saoIncluidos() {
        salvarGasto(new BigDecimal("10.00"), INICIO);   // primeiro dia
        salvarGasto(new BigDecimal("20.00"), FIM);      // último dia

        FluxoCaixaDto fluxo = fluxoCaixaService.calcular(INICIO, FIM);

        assertThat(fluxo.getSaidaGastosVariaveis())
                .isEqualByComparingTo(new BigDecimal("30.00"));
    }

    @Test
    @DisplayName("saidaGastosVariaveis é incluída no totalSaidas")
    void saidaGastos_integraAoTotalSaidas() {
        salvarGasto(new BigDecimal("300.00"), INICIO.plusDays(7));

        FluxoCaixaDto fluxo = fluxoCaixaService.calcular(INICIO, FIM);

        // totalSaidas deve conter ao menos os gastos lançados
        assertThat(fluxo.getTotalSaidas())
                .isGreaterThanOrEqualTo(new BigDecimal("300.00"));
    }

    @Test
    @DisplayName("Múltiplos gastos de categorias distintas são somados corretamente")
    void gastosDeCategoriasDistintas_somamJuntos() {
        gastoRepository.save(GastoVariavel.builder()
                .descricao("Embalagem")
                .valor(new BigDecimal("150.00"))
                .dataLancamento(INICIO.plusDays(2))
                .categoria(CategoriaGasto.EMBALAGEM)
                .referenciaMes(INICIO.getMonthValue())
                .referenciaAno(INICIO.getYear())
                .build());

        gastoRepository.save(GastoVariavel.builder()
                .descricao("Transporte")
                .valor(new BigDecimal("80.00"))
                .dataLancamento(INICIO.plusDays(5))
                .categoria(CategoriaGasto.TRANSPORTE)
                .referenciaMes(INICIO.getMonthValue())
                .referenciaAno(INICIO.getYear())
                .build());

        gastoRepository.save(GastoVariavel.builder()
                .descricao("Marketing")
                .valor(new BigDecimal("220.00"))
                .dataLancamento(INICIO.plusDays(15))
                .categoria(CategoriaGasto.MARKETING)
                .referenciaMes(INICIO.getMonthValue())
                .referenciaAno(INICIO.getYear())
                .build());

        FluxoCaixaDto fluxo = fluxoCaixaService.calcular(INICIO, FIM);

        assertThat(fluxo.getSaidaGastosVariaveis())
                .isEqualByComparingTo(new BigDecimal("450.00"));
    }
}
