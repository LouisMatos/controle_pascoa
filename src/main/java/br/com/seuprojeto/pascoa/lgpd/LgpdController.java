package br.com.seuprojeto.pascoa.lgpd;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/lgpd")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class LgpdController {

    private final LgpdService lgpdService;

    // ── Painel principal ─────────────────────────────────────────────────────

    @GetMapping
    public String painel(Model model) {
        var clientes = lgpdService.listarTodos();
        model.addAttribute("clientes", clientes);
        model.addAttribute("totalOptIn",      clientes.stream().filter(c -> Boolean.TRUE.equals(c.getOptIn())).count());
        model.addAttribute("totalOptOut",     clientes.stream().filter(c -> !Boolean.TRUE.equals(c.getOptIn())).count());
        model.addAttribute("totalAnonimizados", clientes.stream().filter(c -> Boolean.TRUE.equals(c.getAnonimizado())).count());
        return "lgpd/painel";
    }

    // ── Consentimento ─────────────────────────────────────────────────────────

    @PostMapping("/clientes/{id}/consentimento")
    public String atualizarConsentimento(@PathVariable Long id,
                                         @RequestParam boolean optIn,
                                         RedirectAttributes ra) {
        lgpdService.registrarConsentimento(id, optIn);
        ra.addFlashAttribute("sucesso",
            optIn ? "Consentimento registrado com sucesso." : "Opt-out registrado com sucesso.");
        return "redirect:/lgpd";
    }

    // ── Anonimização ──────────────────────────────────────────────────────────

    @PostMapping("/clientes/{id}/anonimizar")
    public String anonimizar(@PathVariable Long id, RedirectAttributes ra) {
        try {
            lgpdService.anonimizar(id);
            ra.addFlashAttribute("sucesso", "Dados do titular anonimizados com sucesso.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/lgpd";
    }

    // ── Exportação (portabilidade) ────────────────────────────────────────────

    @GetMapping("/clientes/{id}/exportar")
    public ResponseEntity<byte[]> exportar(@PathVariable Long id) {
        byte[] dados = lgpdService.exportarDados(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentDisposition(ContentDisposition.attachment()
            .filename("dados-titular-" + id + ".json")
            .build());
        return ResponseEntity.ok()
            .headers(headers)
            .body(dados);
    }
}
