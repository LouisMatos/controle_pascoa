package br.com.seuprojeto.pascoa.analytics.controller;

import br.com.seuprojeto.pascoa.analytics.dto.*;
import br.com.seuprojeto.pascoa.analytics.service.AnalyticsService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Controller
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final ObjectMapper objectMapper;

    @GetMapping
    public String dashboard(@RequestParam(required = false) Integer ano, Model model)
            throws JsonProcessingException {

        List<Integer> anos = analyticsService.anosDisponiveis();
        if (anos.isEmpty()) anos = List.of(java.time.LocalDate.now().getYear());

        int anoSelecionado = (ano != null) ? ano : anos.get(0);

        SafraDto safraAtual = analyticsService.safra(anoSelecionado);
        ComparativoSafraDto comparativo = analyticsService.comparativo();
        List<RankingProdutoDto> ranking = analyticsService.rankingProdutos(anoSelecionado);

        // ── Ticket médio (pré-calculado para evitar SpEL complexo) ──────────
        BigDecimal ticketMedio = safraAtual.totalPedidos() > 0
                ? safraAtual.totalFaturamento()
                      .divide(BigDecimal.valueOf(safraAtual.totalPedidos()), 2, RoundingMode.HALF_UP)
                : null;

        // ── Variação percentual ───────────────────────────────────────────────
        BigDecimal variacaoPercent = comparativo.variacaoFaturamento();
        boolean variacaoPositiva   = variacaoPercent.compareTo(BigDecimal.ZERO) >= 0;

        // ── Dados para Chart.js (séries mensais, até 2 safras) ───────────────
        List<String> labelsMeses = safraAtual.meses().stream()
                .map(MesDto::nomeMes)
                .toList();

        List<BigDecimal> fatAtual = safraAtual.meses().stream()
                .map(MesDto::faturamento)
                .toList();

        // Safra anterior (se existir)
        List<BigDecimal> fatAnterior = List.of();
        int anoAnterior = 0;
        if (comparativo.safras().size() >= 2) {
            SafraDto safraAnt = comparativo.safras().get(1);
            if (safraAnt.ano() != anoSelecionado) {
                anoAnterior = safraAnt.ano();
                fatAnterior = safraAnt.meses().stream()
                        .map(MesDto::faturamento)
                        .toList();
            }
        }

        // Serializa para JSON — Chart.js lê diretamente via th:inline="javascript"
        model.addAttribute("anos", anos);
        model.addAttribute("anoSelecionado", anoSelecionado);
        model.addAttribute("safraAtual", safraAtual);
        model.addAttribute("ranking", ranking);
        model.addAttribute("ticketMedio", ticketMedio);
        model.addAttribute("variacaoPercent", variacaoPercent.abs());
        model.addAttribute("variacaoPositiva", variacaoPositiva);
        model.addAttribute("jsonLabelsMeses", objectMapper.writeValueAsString(labelsMeses));
        model.addAttribute("jsonFatAtual", objectMapper.writeValueAsString(fatAtual));
        model.addAttribute("jsonFatAnterior", objectMapper.writeValueAsString(fatAnterior));
        model.addAttribute("anoAnterior", anoAnterior);

        return "analytics/dashboard";
    }
}
