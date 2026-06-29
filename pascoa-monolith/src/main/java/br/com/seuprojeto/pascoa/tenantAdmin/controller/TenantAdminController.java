package br.com.seuprojeto.pascoa.tenantAdmin.controller;

import br.com.seuprojeto.pascoa.tenantAdmin.util.TenantColorValidator;
import br.com.seuprojeto.pascoa.tenantAdmin.util.TenantColorValidator.ContrastResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

/**
 * Painel Admin do Tenant — FASE 7 do design v7.
 *
 * Cinco abas: Identidade Visual, Fases de Produção, Atributos de
 * Produto, Templates de Notificação, Plano / Upgrade.
 *
 * Esta primeira versão usa dados mock para destravar a UI; as ações de
 * POST flasham mensagem de sucesso sem persistir. Quando o monólito for
 * multi-tenantizado (Etapa 16 v6 — tenant Páscoa Original), os métodos
 * GET/POST passarão a consumir tenant-service, config-engine-service e
 * subscription-service via HTTP/Feign.
 */
@Controller
@RequestMapping("/admin/tenant")
@RequiredArgsConstructor
public class TenantAdminController {

    private final TenantColorValidator colorValidator;

    // ════════════════════════════════════════════════════════════════
    // 7.1 — Identidade Visual
    // ════════════════════════════════════════════════════════════════

    @GetMapping("/identidade")
    public String identidade(Model model) {
        model.addAttribute("nomeApp",       "Páscoa Gestão");
        model.addAttribute("corPrimaria",   "#e91e63");
        model.addAttribute("corSecundaria", "#fce4ec");
        model.addAttribute("logoUrl",       null);
        return "admin/tenant/identidade";
    }

    // W-07 — rejeita nomeApp com caracteres HTML/controle (defesa em profundidade caso
    // algum template futuro use th:utext por engano). Aceita letras, dígitos, espaço e
    // pontuação básica segura.
    private static final java.util.regex.Pattern NOME_APP_SEGURO =
            java.util.regex.Pattern.compile("^[\\p{L}\\p{N} ._'\\-&]{1,80}$");

    @PostMapping("/identidade")
    public String salvarIdentidade(@RequestParam(required = false) String nomeApp,
                                    @RequestParam(required = false) String corPrimaria,
                                    @RequestParam(required = false) String corSecundaria,
                                    RedirectAttributes ra) {
        // W-01 — validar formato #RRGGBB antes de persistir (bloqueia CSS injection).
        try {
            if (corPrimaria   != null) colorValidator.validarFormato(corPrimaria);
            if (corSecundaria != null) colorValidator.validarFormato(corSecundaria);
        } catch (TenantColorValidator.CorInvalidaException e) {
            ra.addFlashAttribute("erro", e.getMessage());
            return "redirect:/admin/tenant/identidade";
        }
        // W-07 — rejeita nomeApp com HTML/script (<, >, etc.).
        if (nomeApp != null && !nomeApp.isBlank() && !NOME_APP_SEGURO.matcher(nomeApp).matches()) {
            ra.addFlashAttribute("erro",
                    "Nome do app contém caracteres inválidos. Use apenas letras, dígitos, espaços e . _ ' - &");
            return "redirect:/admin/tenant/identidade";
        }
        // TODO: persistir em ConfiguracaoSistema ou em tenant-service quando multi-tenant
        ra.addFlashAttribute("sucesso",
                "Identidade visual salva (preview — persistência será habilitada na multi-tenantização).");
        return "redirect:/admin/tenant/identidade";
    }

    /**
     * Endpoint AJAX — validador de contraste WCAG (7.6).
     * Chamado pelo Alpine.js a cada mudança de cor.
     */
    @GetMapping(value = "/identidade/contraste", produces = "application/json")
    @ResponseBody
    public ContrastResult validarContraste(@RequestParam(defaultValue = "#ffffff") String fg,
                                            @RequestParam(defaultValue = "#e91e63") String bg) {
        return colorValidator.validar(fg, bg);
    }

    // ════════════════════════════════════════════════════════════════
    // 7.2 — Fases de Produção
    // ════════════════════════════════════════════════════════════════

    @GetMapping("/fases")
    public String fases(Model model) {
        model.addAttribute("fases", List.of(
                Map.of("ordem", 1, "nome", "Aguardando Insumos", "cor", "#64748b", "ativa", true),
                Map.of("ordem", 2, "nome", "Em Produção",         "cor", "#d97706", "ativa", true),
                Map.of("ordem", 3, "nome", "Qualidade",            "cor", "#7c3aed", "ativa", true),
                Map.of("ordem", 4, "nome", "Pronto",               "cor", "#16a34a", "ativa", true),
                Map.of("ordem", 5, "nome", "Cancelado",            "cor", "#dc2626", "ativa", false)
        ));
        return "admin/tenant/fases";
    }

    @PostMapping("/fases/salvar")
    public String salvarFases(@RequestParam(required = false) String ordemJson,
                               RedirectAttributes ra) {
        // TODO: cabeamento com config-engine-service (POST /config/{tenantId}/fases-producao)
        ra.addFlashAttribute("sucesso",
                "Ordem das fases salva (preview — wire-up com config-engine-service pendente).");
        return "redirect:/admin/tenant/fases";
    }

    // ════════════════════════════════════════════════════════════════
    // 7.3 — Atributos de Produto
    // ════════════════════════════════════════════════════════════════

    @GetMapping("/atributos")
    public String atributos(Model model) {
        model.addAttribute("atributos", List.of(
                Map.of("id", 1, "nome", "Recheio",      "tipo", "LISTA",  "opcoes", "Doce de leite, Brigadeiro, Maracujá, Limão", "obrigatorio", true),
                Map.of("id", 2, "nome", "Peso (g)",     "tipo", "NUMERO", "opcoes", "",                                            "obrigatorio", true),
                Map.of("id", 3, "nome", "Sem lactose?",  "tipo", "BOOLEAN","opcoes", "",                                            "obrigatorio", false),
                Map.of("id", 4, "nome", "Mensagem extra","tipo", "TEXTO", "opcoes", "",                                            "obrigatorio", false)
        ));
        model.addAttribute("tipos", List.of("TEXTO", "NUMERO", "BOOLEAN", "LISTA", "DATA"));
        return "admin/tenant/atributos";
    }

    @PostMapping("/atributos/salvar")
    public String salvarAtributos(RedirectAttributes ra) {
        ra.addFlashAttribute("sucesso", "Atributos salvos (preview).");
        return "redirect:/admin/tenant/atributos";
    }

    // ════════════════════════════════════════════════════════════════
    // 7.4 — Templates de Notificação
    // ════════════════════════════════════════════════════════════════

    @GetMapping("/notificacoes")
    public String notificacoes(Model model) {
        model.addAttribute("eventos", List.of(
                "PEDIDO_CONFIRMADO", "PRODUCAO_INICIADA", "PEDIDO_PRONTO",
                "PEDIDO_ENTREGUE", "PAGAMENTO_RECEBIDO", "PEDIDO_CANCELADO",
                "ORCAMENTO_APROVADO", "ANIVERSARIO_CLIENTE"
        ));
        model.addAttribute("variaveis", List.of(
                "{cliente_nome}", "{pedido_numero}", "{data_entrega}",
                "{valor_total}", "{link_acompanhamento}", "{produtos_lista}",
                "{tenant_nome}", "{tenant_whatsapp}"
        ));
        model.addAttribute("templateAtual",
                "Olá {cliente_nome}! Seu pedido #{pedido_numero} foi confirmado. "
              + "Total: R$ {valor_total}. Acompanhe em {link_acompanhamento}.");
        model.addAttribute("eventoSelecionado", "PEDIDO_CONFIRMADO");
        model.addAttribute("canalSelecionado",   "WHATSAPP");
        return "admin/tenant/notificacoes";
    }

    @PostMapping("/notificacoes/salvar")
    public String salvarNotificacao(RedirectAttributes ra) {
        ra.addFlashAttribute("sucesso", "Template salvo (preview).");
        return "redirect:/admin/tenant/notificacoes";
    }

    // ════════════════════════════════════════════════════════════════
    // 7.5 — Plano / Upgrade
    // ════════════════════════════════════════════════════════════════

    @GetMapping("/plano")
    public String plano(Model model) {
        model.addAttribute("planoAtual", "ENTERPRISE");
        model.addAttribute("trialAte",   null);
        model.addAttribute("featuresAtivas", List.of(
                Map.of("chave", "CATALOGO_PUBLICO",     "nome", "Catálogo Público",       "ativo", true),
                Map.of("chave", "NOTIFICACAO_WHATSAPP", "nome", "Notificação WhatsApp",   "ativo", true),
                Map.of("chave", "NOTIFICACAO_SMS",      "nome", "SMS Fallback",            "ativo", true),
                Map.of("chave", "ANALYTICS_AVANCADO",   "nome", "Analytics Avançado",     "ativo", true),
                Map.of("chave", "API_PUBLICA",          "nome", "API Pública",             "ativo", true),
                Map.of("chave", "DRE",                  "nome", "DRE",                     "ativo", true),
                Map.of("chave", "MULTI_USUARIO",        "nome", "Usuários ilimitados",    "ativo", true),
                Map.of("chave", "FICHA_TECNICA",        "nome", "Ficha Técnica",          "ativo", true)
        ));
        model.addAttribute("planos", List.of(
                Map.of("codigo", "TRIAL",      "nome", "Trial 14 dias", "preco", "Grátis",      "destaque", false),
                Map.of("codigo", "STARTER",    "nome", "Starter",        "preco", "R$ 99/mês",   "destaque", false),
                Map.of("codigo", "PRO",        "nome", "Pro",            "preco", "R$ 249/mês",  "destaque", true),
                Map.of("codigo", "ENTERPRISE", "nome", "Enterprise",     "preco", "Sob consulta","destaque", false)
        ));
        return "admin/tenant/plano";
    }

    @PostMapping("/plano/upgrade")
    public String upgrade(@RequestParam String codigoPlano, RedirectAttributes ra) {
        ra.addFlashAttribute("sucesso",
                "Solicitação de upgrade para " + codigoPlano + " registrada (Stripe checkout será aberto na integração).");
        return "redirect:/admin/tenant/plano";
    }
}
