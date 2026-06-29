package br.com.seuprojeto.pascoa.onboarding.controller;

import br.com.seuprojeto.pascoa.tenantAdmin.util.TenantColorValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

/**
 * Onboarding Wizard — FASE 8 do design v7.
 *
 * 7 etapas guiadas + tela de conclusão. Cada GET renderiza a etapa
 * correspondente; cada POST avança para a próxima e flasha sucesso.
 *
 * Esta primeira versão usa mock data e NÃO persiste — serve como UI
 * de referência. Quando a v6 multi-tenant for ativada no monólito,
 * cada POST passará a chamar tenant-service / config-engine-service /
 * subscription-service via HTTP.
 *
 * Rota pública liberada em {@code SecurityConfig} para simular signup
 * de um novo cliente sem autenticação.
 */
@Controller
@RequestMapping("/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final TenantColorValidator colorValidator;

    private static final int TOTAL = 7;

    // ════════════════════════════════════════════════════════════════
    // 8.2 — Etapa 1: Dados da Empresa
    // ════════════════════════════════════════════════════════════════
    @GetMapping({"", "/", "/etapa1"})
    public String etapa1(Model model) {
        model.addAttribute("etapaAtual", 1);
        model.addAttribute("totalEtapas", TOTAL);
        return "onboarding/etapa1";
    }

    @PostMapping("/etapa1")
    public String salvarEtapa1(@RequestParam(required = false) String nomeEmpresa,
                                @RequestParam(required = false) String cnpj,
                                @RequestParam(required = false) String email,
                                RedirectAttributes ra) {
        ra.addFlashAttribute("sucesso", "Dados da empresa registrados.");
        return "redirect:/onboarding/etapa2";
    }

    // ════════════════════════════════════════════════════════════════
    // 8.3 — Etapa 2: Tipo de Negócio (6 templates)
    // ════════════════════════════════════════════════════════════════
    @GetMapping("/etapa2")
    public String etapa2(Model model) {
        model.addAttribute("etapaAtual", 2);
        model.addAttribute("totalEtapas", TOTAL);
        model.addAttribute("templates", List.of(
                Map.of("codigo", "CONFEITARIA", "nome", "Confeitaria / Bolos",  "icone", "bi-cake2",        "descricao", "Bolos, doces e ovos artesanais"),
                Map.of("codigo", "MARMITARIA",  "nome", "Marmitaria",            "icone", "bi-egg-fried",    "descricao", "Refeições prontas e fitness"),
                Map.of("codigo", "RESTAURANTE", "nome", "Restaurante / Buffet", "icone", "bi-shop",          "descricao", "Pratos à la carte, buffet, eventos"),
                Map.of("codigo", "SALGADERIA",  "nome", "Salgaderia",            "icone", "bi-emoji-smile",  "descricao", "Salgados, festas e encomendas"),
                Map.of("codigo", "DOCES",       "nome", "Doces Gourmet",         "icone", "bi-flower1",      "descricao", "Trufas, bombons e doces finos"),
                Map.of("codigo", "CUSTOM",      "nome", "Configuração Custom",   "icone", "bi-sliders",      "descricao", "Começar do zero e configurar tudo")
        ));
        return "onboarding/etapa2";
    }

    @PostMapping("/etapa2")
    public String salvarEtapa2(@RequestParam(required = false) String tipoNegocio,
                                RedirectAttributes ra) {
        ra.addFlashAttribute("sucesso", "Template " + tipoNegocio + " aplicado.");
        return "redirect:/onboarding/etapa3";
    }

    // ════════════════════════════════════════════════════════════════
    // 8.4 — Etapa 3: Catálogo Pré-carregado
    // ════════════════════════════════════════════════════════════════
    @GetMapping("/etapa3")
    public String etapa3(Model model) {
        model.addAttribute("etapaAtual", 3);
        model.addAttribute("totalEtapas", TOTAL);
        model.addAttribute("produtos", List.of(
                Map.of("nome", "Ovo Trufado 350g",  "preco", "59,90", "categoria", "TRUFADO"),
                Map.of("nome", "Ovo Recheado 500g", "preco", "79,90", "categoria", "RECHEADO"),
                Map.of("nome", "Ovo Diet 250g",      "preco", "49,90", "categoria", "DIET"),
                Map.of("nome", "Ovo Tradicional 350g","preco","45,00", "categoria", "TRADICIONAL")
        ));
        return "onboarding/etapa3";
    }

    @PostMapping("/etapa3")
    public String salvarEtapa3(RedirectAttributes ra) {
        ra.addFlashAttribute("sucesso", "Catálogo inicial salvo.");
        return "redirect:/onboarding/etapa4";
    }

    // ════════════════════════════════════════════════════════════════
    // 8.5 — Etapa 4: Fases de Produção (reusa Sortable.js de 7.2)
    // ════════════════════════════════════════════════════════════════
    @GetMapping("/etapa4")
    public String etapa4(Model model) {
        model.addAttribute("etapaAtual", 4);
        model.addAttribute("totalEtapas", TOTAL);
        model.addAttribute("fases", List.of(
                Map.of("ordem", 1, "nome", "Aguardando Insumos", "cor", "#64748b"),
                Map.of("ordem", 2, "nome", "Em Produção",         "cor", "#d97706"),
                Map.of("ordem", 3, "nome", "Qualidade",            "cor", "#7c3aed"),
                Map.of("ordem", 4, "nome", "Pronto",               "cor", "#16a34a")
        ));
        return "onboarding/etapa4";
    }

    @PostMapping("/etapa4")
    public String salvarEtapa4(RedirectAttributes ra) {
        ra.addFlashAttribute("sucesso", "Fases de produção configuradas.");
        return "redirect:/onboarding/etapa5";
    }

    // ════════════════════════════════════════════════════════════════
    // 8.6 — Etapa 5: Identidade Visual
    // ════════════════════════════════════════════════════════════════
    @GetMapping("/etapa5")
    public String etapa5(Model model) {
        model.addAttribute("etapaAtual", 5);
        model.addAttribute("totalEtapas", TOTAL);
        return "onboarding/etapa5";
    }

    @PostMapping("/etapa5")
    public String salvarEtapa5(@RequestParam(required = false) String corPrimaria,
                                RedirectAttributes ra) {
        // W-01 — validar formato #RRGGBB antes de prosseguir.
        try {
            if (corPrimaria != null) colorValidator.validarFormato(corPrimaria);
        } catch (TenantColorValidator.CorInvalidaException e) {
            ra.addFlashAttribute("erro", e.getMessage());
            return "redirect:/onboarding/etapa5";
        }
        ra.addFlashAttribute("sucesso", "Identidade visual salva.");
        return "redirect:/onboarding/etapa6";
    }

    // ════════════════════════════════════════════════════════════════
    // 8.7 — Etapa 6: Notificações (WhatsApp + Email)
    // ════════════════════════════════════════════════════════════════
    @GetMapping("/etapa6")
    public String etapa6(Model model) {
        model.addAttribute("etapaAtual", 6);
        model.addAttribute("totalEtapas", TOTAL);
        return "onboarding/etapa6";
    }

    @PostMapping("/etapa6")
    public String salvarEtapa6(RedirectAttributes ra) {
        ra.addFlashAttribute("sucesso", "Notificações configuradas.");
        return "redirect:/onboarding/etapa7";
    }

    // ════════════════════════════════════════════════════════════════
    // 8.8 — Etapa 7: Equipe (convites)
    // ════════════════════════════════════════════════════════════════
    @GetMapping("/etapa7")
    public String etapa7(Model model) {
        model.addAttribute("etapaAtual", 7);
        model.addAttribute("totalEtapas", TOTAL);
        return "onboarding/etapa7";
    }

    @PostMapping("/etapa7")
    public String concluir(RedirectAttributes ra) {
        ra.addFlashAttribute("sucesso", "Onboarding concluído! Bem-vindo ao FoodFlow.");
        return "redirect:/onboarding/conclusao";
    }

    // ════════════════════════════════════════════════════════════════
    // Tela final
    // ════════════════════════════════════════════════════════════════
    @GetMapping("/conclusao")
    public String conclusao(Model model) {
        model.addAttribute("etapaAtual", 8); // todos os steps done
        model.addAttribute("totalEtapas", TOTAL);
        return "onboarding/conclusao";
    }
}
