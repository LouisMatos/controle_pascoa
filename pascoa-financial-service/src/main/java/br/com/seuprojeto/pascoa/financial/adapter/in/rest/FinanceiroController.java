package br.com.seuprojeto.pascoa.financial.adapter.in.rest;

import br.com.seuprojeto.pascoa.financial.adapter.in.rest.dto.LancamentoRequest;
import br.com.seuprojeto.pascoa.financial.application.port.in.FinanceiroUseCase;
import br.com.seuprojeto.pascoa.financial.domain.exception.LancamentoNotFoundException;
import br.com.seuprojeto.pascoa.financial.domain.model.DreAnual;
import br.com.seuprojeto.pascoa.financial.domain.model.Lancamento;
import br.com.seuprojeto.pascoa.financial.domain.model.ResumoFinanceiro;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/financeiro")
@RequiredArgsConstructor
public class FinanceiroController {

    private final FinanceiroUseCase financeiroUseCase;

    @GetMapping("/lancamentos")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCEIRO','ANALISTA')")
    public List<Lancamento> listar(
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().getMonthValue()}") int mes,
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().getYear()}") int ano) {
        return financeiroUseCase.listar(mes, ano);
    }

    @GetMapping("/lancamentos/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCEIRO','ANALISTA')")
    public Lancamento buscar(@PathVariable Long id) {
        return financeiroUseCase.buscarPorId(id);
    }

    @PostMapping("/lancamentos")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCEIRO')")
    public ResponseEntity<Lancamento> registrar(@Valid @RequestBody LancamentoRequest req) {
        var l = financeiroUseCase.registrar(new FinanceiroUseCase.RegistrarLancamentoCommand(
                req.tipo(), req.categoria(), req.descricao(),
                req.valor(), req.data(), req.referenciaId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(l);
    }

    @GetMapping("/resumo")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCEIRO','ANALISTA')")
    public ResumoFinanceiro resumo(
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer ano) {
        YearMonth ym = YearMonth.now();
        return financeiroUseCase.resumo(
                mes != null ? mes : ym.getMonthValue(),
                ano != null ? ano : ym.getYear());
    }

    @GetMapping("/dre")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCEIRO','ANALISTA')")
    public DreAnual dre(@RequestParam(required = false) Integer ano) {
        return financeiroUseCase.dre(ano != null ? ano : LocalDate.now().getYear());
    }

    @GetMapping("/fluxo-caixa")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCEIRO','ANALISTA')")
    public List<ResumoFinanceiro> fluxoCaixa(@RequestParam(required = false) Integer ano) {
        int anoRef = ano != null ? ano : LocalDate.now().getYear();
        return java.util.stream.IntStream.rangeClosed(1, 12)
                .mapToObj(mes -> financeiroUseCase.resumo(mes, anoRef))
                .toList();
    }

    @ExceptionHandler(LancamentoNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(LancamentoNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }
}
