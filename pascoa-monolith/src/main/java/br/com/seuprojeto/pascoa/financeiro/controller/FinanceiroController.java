package br.com.seuprojeto.pascoa.financeiro.controller;

import br.com.seuprojeto.pascoa.financeiro.entity.StatusConta;
import br.com.seuprojeto.pascoa.financeiro.repository.ContaPagarRepository;
import br.com.seuprojeto.pascoa.financeiro.service.BreakevenService;
import br.com.seuprojeto.pascoa.financeiro.service.CustoRealService;
import br.com.seuprojeto.pascoa.financeiro.service.FinanceiroService;
import br.com.seuprojeto.pascoa.financeiro.service.FluxoCaixaService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.YearMonth;

@Controller
@RequestMapping("/financeiro")
@RequiredArgsConstructor
public class FinanceiroController {

    private final FinanceiroService financeiroService;
    private final CustoRealService custoRealService;
    private final FluxoCaixaService fluxoCaixaService;
    private final BreakevenService breakevenService;
    private final ContaPagarRepository contaPagarRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("relatorio", financeiroService.gerarRelatorio());
        return "financeiro/dashboard";
    }

    @GetMapping("/custo-real/{id}")
    public String custoReal(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("custo", custoRealService.calcular(id));
            return "financeiro/custo-real";
        } catch (IllegalArgumentException | jakarta.persistence.EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("erro",
                "Pedido #" + id + " não encontrado. Verifique se o pedido existe.");
            return "redirect:/financeiro/dashboard";
        }
    }

    @GetMapping("/fluxo-caixa")
    public String fluxoCaixa(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            Model model) {
        if (inicio == null) inicio = YearMonth.now().atDay(1);
        if (fim == null)    fim    = YearMonth.now().atEndOfMonth();
        model.addAttribute("fluxo", fluxoCaixaService.calcular(inicio, fim));
        return "financeiro/fluxo-caixa";
    }

    @GetMapping("/breakeven")
    public String breakeven(Model model) {
        model.addAttribute("breakeven", breakevenService.calcular());
        return "financeiro/breakeven";
    }

    @GetMapping("/projecao-safra")
    public String projecaoSafra(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            Model model) {
        if (inicio == null) inicio = LocalDate.now().withDayOfYear(1);
        if (fim == null)    fim    = LocalDate.now();
        model.addAttribute("projecao", breakevenService.projecaoSafra(inicio, fim));
        return "financeiro/projecao-safra";
    }

    @GetMapping("/aging")
    public String aging(Model model) {
        model.addAttribute("aging", breakevenService.aging());
        return "financeiro/aging";
    }

    @GetMapping("/contas-pagar")
    public String contasPagar(
            @RequestParam(required = false) StatusConta status,
            Model model) {
        var contas = status != null
            ? contaPagarRepository.findByStatusOrderByVencimento(status)
            : contaPagarRepository.findAllByOrderByVencimento();

        var hoje = java.time.LocalDate.now();
        var em7Dias = hoje.plusDays(7);

        java.math.BigDecimal totalValor = contas.stream()
            .map(c -> c.getValor())
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        long qtdVencidas = contas.stream().filter(c -> c.isVencida()).count();
        long qtdVence7Dias = contas.stream()
            .filter(c -> c.getStatus() == StatusConta.ABERTA && !c.isVencida()
                && c.getVencimento().isBefore(em7Dias))
            .count();

        model.addAttribute("contas", contas);
        model.addAttribute("statusAtual", status);
        model.addAttribute("statusValues", StatusConta.values());
        model.addAttribute("totalValor", totalValor);
        model.addAttribute("qtdVencidas", qtdVencidas);
        model.addAttribute("qtdVence7Dias", qtdVence7Dias);
        return "financeiro/contas-pagar";
    }
}
