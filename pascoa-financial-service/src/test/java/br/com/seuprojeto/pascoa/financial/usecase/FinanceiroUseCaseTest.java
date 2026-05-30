package br.com.seuprojeto.pascoa.financial.usecase;

import br.com.seuprojeto.pascoa.financial.application.port.in.FinanceiroUseCase.RegistrarLancamentoCommand;
import br.com.seuprojeto.pascoa.financial.application.port.out.LancamentoRepositoryPort;
import br.com.seuprojeto.pascoa.financial.application.usecase.FinanceiroUseCaseImpl;
import br.com.seuprojeto.pascoa.financial.domain.model.*;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinanceiroUseCaseTest {

    @Mock LancamentoRepositoryPort repository;

    private FinanceiroUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new FinanceiroUseCaseImpl(repository);
    }

    @Test
    @DisplayName("Registrar lançamento manual salva corretamente")
    void registrarLancamento() {
        when(repository.save(any())).thenAnswer(inv -> {
            Lancamento l = inv.getArgument(0);
            return l.withId(1L);
        });

        Lancamento salvo = useCase.registrar(new RegistrarLancamentoCommand(
                TipoLancamento.RECEITA, CategoriaLancamento.VENDA,
                "Venda Pedido #10", new BigDecimal("150.00"),
                LocalDate.now(), "10"));

        assertThat(salvo.getTipo()).isEqualTo(TipoLancamento.RECEITA);
        assertThat(salvo.getValor()).isEqualByComparingTo("150.00");
        assertThat(salvo.getOrigem()).isEqualTo("manual");
    }

    @Test
    @DisplayName("ResumoFinanceiro calcula lucro e margem corretamente")
    void resumoFinanceiro() {
        List<Lancamento> lancamentos = List.of(
                lancamento(TipoLancamento.RECEITA, "500.00"),
                lancamento(TipoLancamento.RECEITA, "300.00"),
                lancamento(TipoLancamento.DESPESA, "200.00")
        );
        when(repository.findByMesAno(4, 2026)).thenReturn(lancamentos);

        ResumoFinanceiro resumo = useCase.resumo(4, 2026);

        assertThat(resumo.getTotalReceitas()).isEqualByComparingTo("800.00");
        assertThat(resumo.getTotalDespesas()).isEqualByComparingTo("200.00");
        assertThat(resumo.getLucroLiquido()).isEqualByComparingTo("600.00");
        assertThat(resumo.getMargemLucro()).isEqualByComparingTo("75.00");
    }

    @Test
    @DisplayName("ResumoFinanceiro com receita zero retorna margem zero sem divisão por zero")
    void resumoSemReceita() {
        when(repository.findByMesAno(1, 2026)).thenReturn(List.of(
                lancamento(TipoLancamento.DESPESA, "100.00")
        ));

        ResumoFinanceiro resumo = useCase.resumo(1, 2026);

        assertThat(resumo.getTotalReceitas()).isEqualByComparingTo("0.00");
        assertThat(resumo.getLucroLiquido()).isEqualByComparingTo("-100.00");
        assertThat(resumo.getMargemLucro()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("DRE anual agrega corretamente por categoria")
    void dreAnual() {
        List<Lancamento> todos = List.of(
                lancamentoCateg(TipoLancamento.RECEITA, CategoriaLancamento.VENDA, "1000.00"),
                lancamentoCateg(TipoLancamento.DESPESA, CategoriaLancamento.INSUMO, "300.00"),
                lancamentoCateg(TipoLancamento.DESPESA, CategoriaLancamento.OPERACIONAL, "100.00")
        );
        when(repository.findByAno(2026)).thenReturn(todos);
        when(repository.findByMesAno(anyInt(), eq(2026))).thenReturn(List.of());

        DreAnual dre = useCase.dre(2026);

        assertThat(dre.getReceitaBruta()).isEqualByComparingTo("1000.00");
        assertThat(dre.getCustosVariaveis()).isEqualByComparingTo("300.00");
        assertThat(dre.getLucroBruto()).isEqualByComparingTo("700.00");
        assertThat(dre.getDespesasOperacionais()).isEqualByComparingTo("100.00");
        assertThat(dre.getLucroLiquido()).isEqualByComparingTo("600.00");
    }

    @Test
    @DisplayName("Lancamento.isReceita e isDespesa funcionam corretamente")
    void tipoLancamento() {
        Lancamento r = lancamento(TipoLancamento.RECEITA, "10.00");
        Lancamento d = lancamento(TipoLancamento.DESPESA, "10.00");

        assertThat(r.isReceita()).isTrue();
        assertThat(r.isDespesa()).isFalse();
        assertThat(d.isDespesa()).isTrue();
    }

    private Lancamento lancamento(TipoLancamento tipo, String valor) {
        return Lancamento.builder()
                .tipo(tipo).categoria(CategoriaLancamento.VENDA)
                .descricao("desc").valor(new BigDecimal(valor))
                .data(LocalDate.of(2026, 4, 1)).origem("test").build();
    }

    private Lancamento lancamentoCateg(TipoLancamento tipo, CategoriaLancamento cat, String valor) {
        return Lancamento.builder()
                .tipo(tipo).categoria(cat)
                .descricao("desc").valor(new BigDecimal(valor))
                .data(LocalDate.of(2026, 4, 1)).origem("test").build();
    }
}
