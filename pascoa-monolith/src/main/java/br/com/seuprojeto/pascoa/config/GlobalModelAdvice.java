package br.com.seuprojeto.pascoa.config;

import br.com.seuprojeto.pascoa.notificacao.entity.AlertaInterno;
import br.com.seuprojeto.pascoa.notificacao.service.AlertaInternoService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Injeta dados globais (alertas + V9 Etapa 1.3: tenant/featureFlags/activePage/currentYear)
 * no model de todas as páginas. Garante que nenhum template quebre por atributo null.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final AlertaInternoService alertaService;

    @ModelAttribute("alertasRecentes")
    public List<AlertaInterno> alertasRecentes() {
        if (!isAuthenticated()) return Collections.emptyList();
        try {
            return alertaService.recentes();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @ModelAttribute("alertasNaoLidos")
    public long alertasNaoLidos() {
        if (!isAuthenticated()) return 0L;
        try {
            return alertaService.contarNaoLidos();
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * V9 Etapa 1.3 — Tenant default em todo request. No monólito single-tenant
     * sempre retorna {@link TenantInfo#defaultTenant()}; quando o monólito passar
     * a resolver tenant por subdomínio, este método trocará por lookup real.
     */
    @ModelAttribute("tenant")
    public TenantInfo tenant() {
        return TenantInfo.defaultTenant();
    }

    @ModelAttribute("featureFlags")
    public Map<String, Boolean> featureFlags() {
        return Map.of();
    }

    @ModelAttribute("currentYear")
    public int currentYear() {
        return LocalDate.now().getYear();
    }

    /**
     * URI atual da requisição. Substitui o objeto de expressão {@code #httpServletRequest}
     * do Thymeleaf, removido na versão 3.1 (bundle do Spring Boot 3.x). O fragmento
     * {@code fragments/breadcrumb} consome este atributo para montar a trilha.
     */
    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null ? uri : "/";
    }

    @ModelAttribute("activePage")
    public String activePage(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) return "other";
        if (path.startsWith("/dashboard"))    return "dashboard";
        if (path.startsWith("/pedidos"))      return "pedidos";
        if (path.startsWith("/orcamentos"))   return "orcamentos";
        if (path.startsWith("/producao"))     return "producao";
        if (path.startsWith("/qualidade"))    return "qualidade";
        if (path.startsWith("/produtos"))     return "produtos";
        if (path.startsWith("/catalogo"))     return "catalogo";
        if (path.startsWith("/estoque") ||
            path.startsWith("/materias-primas")) return "estoque";
        if (path.startsWith("/fornecedores")) return "fornecedores";
        if (path.startsWith("/fichas"))       return "fichas";
        if (path.startsWith("/financeiro"))   return "financeiro";
        if (path.startsWith("/gastos"))       return "gastos";
        if (path.startsWith("/crm") ||
            path.startsWith("/clientes"))     return "crm";
        if (path.startsWith("/analytics"))    return "analytics";
        if (path.startsWith("/notificacoes")) return "notificacoes";
        if (path.startsWith("/usuarios"))     return "usuarios";
        if (path.startsWith("/auditoria"))    return "auditoria";
        if (path.startsWith("/lgpd"))         return "lgpd";
        if (path.startsWith("/admin"))        return "admin";
        return "other";
    }

    private boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal());
    }
}
