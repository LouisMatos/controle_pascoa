package br.com.seuprojeto.pascoa.gastos.controller;

import br.com.seuprojeto.pascoa.gastos.dto.GastoDashboardDto;
import br.com.seuprojeto.pascoa.gastos.dto.GastoVariavelForm;
import br.com.seuprojeto.pascoa.gastos.dto.OrcamentoGastoForm;
import br.com.seuprojeto.pascoa.gastos.entity.CategoriaGasto;
import br.com.seuprojeto.pascoa.gastos.entity.GastoVariavel;
import br.com.seuprojeto.pascoa.gastos.service.GastoVariavelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.YearMonth;
import java.util.List;

@Controller
@RequestMapping("/gastos")
@RequiredArgsConstructor
public class GastoVariavelController {

    private final GastoVariavelService service;

    // ── Lista ──────────────────────────────────────────────────────────────

    @GetMapping
    public String lista(
            @RequestParam(required = false) Integer ano,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) CategoriaGasto categoria,
            Model model) {

        YearMonth atual = YearMonth.now();
        if (ano == null) ano = atual.getYear();
        if (mes == null) mes = atual.getMonthValue();

        List<GastoVariavel> gastos = service.listar(ano, mes, categoria);

        model.addAttribute("gastos", gastos);
        model.addAttribute("anoAtual", ano);
        model.addAttribute("mesAtual", mes);
        model.addAttribute("categoriaAtual", categoria);
        model.addAttribute("categorias", CategoriaGasto.values());
        model.addAttribute("totalMes", gastos.stream()
                .map(GastoVariavel::getValor)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
        return "gastos/lista";
    }

    // ── Novo ───────────────────────────────────────────────────────────────

    @GetMapping("/novo")
    public String novoForm(Model model) {
        model.addAttribute("form", new GastoVariavelForm());
        model.addAttribute("categorias", CategoriaGasto.values());
        return "gastos/form";
    }

    @PostMapping("/novo")
    public String novo(
            @Valid @ModelAttribute("form") GastoVariavelForm form,
            BindingResult result,
            @AuthenticationPrincipal UserDetails usuario,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (result.hasErrors()) {
            model.addAttribute("categorias", CategoriaGasto.values());
            return "gastos/form";
        }
        service.salvar(form, usuario.getUsername());
        redirectAttributes.addFlashAttribute("sucesso", "Gasto registrado com sucesso.");
        return "redirect:/gastos";
    }

    // ── Editar ─────────────────────────────────────────────────────────────

    @GetMapping("/{id}/editar")
    public String editarForm(@PathVariable Long id, Model model) {
        GastoVariavel gasto = service.buscarPorId(id);
        GastoVariavelForm form = new GastoVariavelForm();
        form.setDescricao(gasto.getDescricao());
        form.setValor(gasto.getValor());
        form.setDataLancamento(gasto.getDataLancamento());
        form.setCategoria(gasto.getCategoria());
        form.setObservacoes(gasto.getObservacoes());

        model.addAttribute("form", form);
        model.addAttribute("gastoId", id);
        model.addAttribute("categorias", CategoriaGasto.values());
        return "gastos/form";
    }

    @PostMapping("/{id}/editar")
    public String editar(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") GastoVariavelForm form,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (result.hasErrors()) {
            model.addAttribute("gastoId", id);
            model.addAttribute("categorias", CategoriaGasto.values());
            return "gastos/form";
        }
        service.atualizar(id, form);
        redirectAttributes.addFlashAttribute("sucesso", "Gasto atualizado com sucesso.");
        return "redirect:/gastos";
    }

    // ── Excluir ────────────────────────────────────────────────────────────

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        service.excluir(id);
        redirectAttributes.addFlashAttribute("sucesso", "Gasto excluído.");
        return "redirect:/gastos";
    }

    // ── Dashboard ──────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public String dashboard(
            @RequestParam(required = false) Integer ano,
            @RequestParam(required = false) Integer mes,
            Model model) {

        YearMonth atual = YearMonth.now();
        if (ano == null) ano = atual.getYear();
        if (mes == null) mes = atual.getMonthValue();

        GastoDashboardDto dashboard = service.gerarDashboard(ano, mes);

        // Dados pré-computados para evitar lambdas no SpEL do Thymeleaf
        long estouradosCount = dashboard.porCategoria().stream()
                .filter(GastoDashboardDto.CategoriaResumo::estourado).count();
        java.math.BigDecimal saldo = dashboard.totalOrcado().subtract(dashboard.totalRealizado());
        List<String> chartLabels = dashboard.porCategoria().stream()
                .map(r -> r.categoria().getDescricao()).toList();
        List<java.math.BigDecimal> chartRealizado = dashboard.porCategoria().stream()
                .map(GastoDashboardDto.CategoriaResumo::realizado).toList();
        List<java.math.BigDecimal> chartOrcado = dashboard.porCategoria().stream()
                .map(GastoDashboardDto.CategoriaResumo::orcado).toList();

        model.addAttribute("dashboard", dashboard);
        model.addAttribute("anoAtual", ano);
        model.addAttribute("mesAtual", mes);
        model.addAttribute("estouradosCount", estouradosCount);
        model.addAttribute("saldoTotal", saldo);
        model.addAttribute("chartLabels", chartLabels);
        model.addAttribute("chartRealizado", chartRealizado);
        model.addAttribute("chartOrcado", chartOrcado);
        model.addAttribute("orcamentos", service.listarOrcamentos(ano, mes));
        model.addAttribute("orcamentoForm", new OrcamentoGastoForm());
        model.addAttribute("categorias", CategoriaGasto.values());
        return "gastos/dashboard";
    }

    // ── Orçamento ──────────────────────────────────────────────────────────

    @PostMapping("/orcamento")
    public String salvarOrcamento(
            @Valid @ModelAttribute("orcamentoForm") OrcamentoGastoForm form,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("erro", "Dados do orçamento inválidos.");
        } else {
            service.salvarOrcamento(form);
            redirectAttributes.addFlashAttribute("sucesso", "Orçamento salvo.");
        }
        return "redirect:/gastos/dashboard?ano=" + form.getReferenciaAno()
                + "&mes=" + form.getReferenciaMes();
    }

    // ── Importar CSV ───────────────────────────────────────────────────────

    @GetMapping("/importar")
    public String importarForm() {
        return "gastos/importar";
    }

    @PostMapping("/importar")
    public String importar(
            @RequestParam("arquivo") MultipartFile arquivo,
            @AuthenticationPrincipal UserDetails usuario,
            RedirectAttributes redirectAttributes) {

        if (arquivo.isEmpty()) {
            redirectAttributes.addFlashAttribute("erro", "Selecione um arquivo CSV.");
            return "redirect:/gastos/importar";
        }
        try {
            int total = service.importarCSV(arquivo, usuario.getUsername());
            redirectAttributes.addFlashAttribute("sucesso",
                    total + " gasto(s) importado(s) com sucesso.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro",
                    "Erro ao processar o arquivo: " + e.getMessage());
        }
        return "redirect:/gastos";
    }
}
