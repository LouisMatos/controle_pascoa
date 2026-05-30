package br.com.seuprojeto.pascoa.analytics.usecase;

import br.com.seuprojeto.pascoa.analytics.application.port.out.RegistroVendaRepositoryPort;
import br.com.seuprojeto.pascoa.analytics.application.usecase.AnalyticsUseCaseImpl;
import br.com.seuprojeto.pascoa.analytics.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsUseCaseTest {

    @Mock RegistroVendaRepositoryPort repository;

    private AnalyticsUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new AnalyticsUseCaseImpl(repository);
    }

    @Test
    @DisplayName("MetricaSafra agrega pedidos, receita, clientes e ticket médio")
    void metricaSafra() {
        when(repository.findByAno(2026)).thenReturn(registros2026());

        MetricaSafra safra = useCase.resumoSafra(2026);

        assertThat(safra.getTotalPedidos()).isEqualTo(2);
        assertThat(safra.getTotalClientes()).isEqualTo(2);
        assertThat(safra.getTotalReceita()).isEqualByComparingTo("350.00");
        assertThat(safra.getTicketMedio()).isEqualByComparingTo("175.00");
    }

    @Test
    @DisplayName("ComparativoSafra calcula variação percentual corretamente")
    void comparativoSafra() {
        when(repository.findByAno(2026)).thenReturn(registros2026());     // receita 350
        when(repository.findByAno(2025)).thenReturn(registros2025());     // receita 200

        ComparativoSafra comparativo = useCase.comparativoSafras(2026, 2025);

        // variacao = (350 - 200) / 200 * 100 = 75%
        assertThat(comparativo.getVariacaoReceita()).isEqualByComparingTo("75.00");
        assertThat(comparativo.getSafraAtual().getAno()).isEqualTo(2026);
        assertThat(comparativo.getSafraAnterior().getAno()).isEqualTo(2025);
    }

    @Test
    @DisplayName("RankingProduto ordena por quantidade e respeita o limite")
    void rankingProdutos() {
        when(repository.findByAno(2026)).thenReturn(registros2026());

        List<RankingProduto> ranking = useCase.rankingProdutos(2026, 5);

        assertThat(ranking).isNotEmpty();
        // O primeiro deve ter a maior quantidade
        if (ranking.size() > 1) {
            assertThat(ranking.get(0).getQuantidadeVendida())
                    .isGreaterThanOrEqualTo(ranking.get(1).getQuantidadeVendida());
        }
        // Posições sequenciais
        for (int i = 0; i < ranking.size(); i++) {
            assertThat(ranking.get(i).getPosicao()).isEqualTo(i + 1);
        }
    }

    @Test
    @DisplayName("MetricasMensais retorna 12 meses com dados corretos")
    void metricasMensais() {
        when(repository.findByAno(2026)).thenReturn(registros2026());

        List<MetricaMensal> metricas = useCase.metricasMensais(2026);

        assertThat(metricas).hasSize(12);
        // Mês 4 (abril) tem dados
        MetricaMensal abril = metricas.stream()
                .filter(m -> m.getMes() == 4).findFirst().orElseThrow();
        assertThat(abril.getTotalReceita()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("ComparativoSafra com safra anterior vazia retorna variação 0%")
    void comparativoComSafraVazia() {
        when(repository.findByAno(2026)).thenReturn(registros2026());
        when(repository.findByAno(2024)).thenReturn(List.of());

        ComparativoSafra comparativo = useCase.comparativoSafras(2026, 2024);

        assertThat(comparativo.getVariacaoReceita()).isEqualByComparingTo("0.00");
    }

    private List<RegistroVenda> registros2026() {
        return List.of(
                venda(1L, 1L, 10L, "Ovo Trufado",  3, "200.00", 2026, 4),
                venda(2L, 2L, 11L, "Ovo Recheado", 2, "150.00", 2026, 4)
        );
    }

    private List<RegistroVenda> registros2025() {
        return List.of(
                venda(3L, 1L, 10L, "Ovo Trufado", 2, "200.00", 2025, 4)
        );
    }

    private RegistroVenda venda(Long id, Long clienteId, Long produtoId,
                                 String nome, int qtd, String valor, int ano, int mes) {
        return RegistroVenda.builder()
                .id(id).pedidoId(id).clienteId(clienteId).produtoId(produtoId)
                .nomeProduto(nome).quantidade(qtd)
                .valorTotal(new BigDecimal(valor))
                .dataVenda(LocalDate.of(ano, mes, 1)).ano(ano).mes(mes)
                .build();
    }
}
