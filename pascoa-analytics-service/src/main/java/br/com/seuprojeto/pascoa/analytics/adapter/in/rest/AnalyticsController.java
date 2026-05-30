package br.com.seuprojeto.pascoa.analytics.adapter.in.rest;

import br.com.seuprojeto.pascoa.analytics.application.port.in.AnalyticsUseCase;
import br.com.seuprojeto.pascoa.analytics.domain.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsUseCase analyticsUseCase;

    @GetMapping("/safras/comparativo")
    @PreAuthorize("hasAnyRole('ADMIN','ANALISTA','FINANCEIRO')")
    public ComparativoSafra comparativo(
            @RequestParam(required = false) Integer anoAtual,
            @RequestParam(required = false) Integer anoAnterior) {
        int atual    = anoAtual    != null ? anoAtual    : LocalDate.now().getYear();
        int anterior = anoAnterior != null ? anoAnterior : atual - 1;
        return analyticsUseCase.comparativoSafras(atual, anterior);
    }

    @GetMapping("/safras/{ano}")
    @PreAuthorize("hasAnyRole('ADMIN','ANALISTA','FINANCEIRO')")
    public MetricaSafra resumoSafra(@PathVariable int ano) {
        return analyticsUseCase.resumoSafra(ano);
    }

    @GetMapping("/produtos/ranking")
    @PreAuthorize("hasAnyRole('ADMIN','ANALISTA','FINANCEIRO')")
    public List<RankingProduto> rankingProdutos(
            @RequestParam(required = false) Integer ano,
            @RequestParam(defaultValue = "10") int limite) {
        return analyticsUseCase.rankingProdutos(
                ano != null ? ano : LocalDate.now().getYear(), limite);
    }

    @GetMapping("/metricas-mensais")
    @PreAuthorize("hasAnyRole('ADMIN','ANALISTA','FINANCEIRO')")
    public List<MetricaMensal> metricasMensais(
            @RequestParam(required = false) Integer ano) {
        return analyticsUseCase.metricasMensais(
                ano != null ? ano : LocalDate.now().getYear());
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN','ANALISTA','FINANCEIRO')")
    public Map<String, Object> dashboard() {
        int ano = LocalDate.now().getYear();
        return Map.of(
                "safraAtual",       analyticsUseCase.resumoSafra(ano),
                "comparativo",      analyticsUseCase.comparativoSafras(ano, ano - 1),
                "rankingTop5",      analyticsUseCase.rankingProdutos(ano, 5),
                "metricasMensais",  analyticsUseCase.metricasMensais(ano)
        );
    }
}
