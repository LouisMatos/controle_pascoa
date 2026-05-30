package br.com.seuprojeto.pascoa.crm.controller;

import br.com.seuprojeto.pascoa.crm.dto.ClienteCrmDto;
import br.com.seuprojeto.pascoa.crm.entity.SegmentoCliente;
import br.com.seuprojeto.pascoa.crm.entity.TipoPonto;
import br.com.seuprojeto.pascoa.crm.service.CrmService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/crm")
@RequiredArgsConstructor
public class CrmController {

    private final CrmService service;

    // ── Dashboard / Ranking ────────────────────────────────────────────────

    @GetMapping
    public String dashboard(Model model) {
        List<ClienteCrmDto> ranking = service.gerarRanking();

        // Contadores por segmento
        Map<SegmentoCliente, Long> porSegmento = ranking.stream()
                .collect(Collectors.groupingBy(ClienteCrmDto::segmento, Collectors.counting()));

        long totalClientes = ranking.size();
        long totalVip     = porSegmento.getOrDefault(SegmentoCliente.VIP, 0L);
        long totalNovos   = porSegmento.getOrDefault(SegmentoCliente.NOVO, 0L);
        long totalInativos= porSegmento.getOrDefault(SegmentoCliente.INATIVO, 0L);
        long totalRegulares=porSegmento.getOrDefault(SegmentoCliente.REGULAR, 0L);

        model.addAttribute("ranking", ranking.stream().limit(20).toList());
        model.addAttribute("totalClientes", totalClientes);
        model.addAttribute("totalVip", totalVip);
        model.addAttribute("totalNovos", totalNovos);
        model.addAttribute("totalInativos", totalInativos);
        model.addAttribute("totalRegulares", totalRegulares);
        model.addAttribute("segmentos", SegmentoCliente.values());
        return "crm/dashboard";
    }

    // ── Perfil do cliente ──────────────────────────────────────────────────

    @GetMapping("/clientes/{id}")
    public String perfil(@PathVariable Long id, Model model) {
        ClienteCrmDto perfil = service.perfilCliente(id);
        model.addAttribute("perfil", perfil);
        model.addAttribute("cliente", perfil.cliente());
        model.addAttribute("notas", service.notasCliente(id));
        model.addAttribute("pontos", service.pontosCliente(id));
        model.addAttribute("ultimosPedidos", service.ultimosPedidos(id));
        model.addAttribute("tiposPonto", TipoPonto.values());
        return "crm/perfil";
    }

    // ── Notas ─────────────────────────────────────────────────────────────

    @PostMapping("/clientes/{id}/notas")
    public String salvarNota(@PathVariable Long id,
                             @RequestParam String texto,
                             @AuthenticationPrincipal UserDetails usuario,
                             RedirectAttributes ra) {
        if (texto == null || texto.isBlank()) {
            ra.addFlashAttribute("erro", "O texto da nota não pode estar vazio.");
        } else {
            service.salvarNota(id, texto, usuario.getUsername());
            ra.addFlashAttribute("sucesso", "Nota adicionada.");
        }
        return "redirect:/crm/clientes/" + id;
    }

    @PostMapping("/clientes/{id}/notas/{notaId}/excluir")
    public String excluirNota(@PathVariable Long id, @PathVariable Long notaId,
                              RedirectAttributes ra) {
        service.excluirNota(notaId);
        ra.addFlashAttribute("sucesso", "Nota removida.");
        return "redirect:/crm/clientes/" + id;
    }

    // ── Pontos ────────────────────────────────────────────────────────────

    @PostMapping("/clientes/{id}/pontos")
    public String lancarPontos(@PathVariable Long id,
                               @RequestParam int pontos,
                               @RequestParam TipoPonto tipo,
                               @RequestParam(required = false) String descricao,
                               @AuthenticationPrincipal UserDetails usuario,
                               RedirectAttributes ra) {
        try {
            service.lancarPontos(id, pontos, tipo, descricao, usuario.getUsername());
            ra.addFlashAttribute("sucesso",
                    (tipo == TipoPonto.CREDITO ? "+" : "-") + pontos + " pontos lançados.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/crm/clientes/" + id;
    }
}
