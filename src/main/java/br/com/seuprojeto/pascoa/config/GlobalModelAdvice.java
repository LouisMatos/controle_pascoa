package br.com.seuprojeto.pascoa.config;

import br.com.seuprojeto.pascoa.notificacao.entity.AlertaInterno;
import br.com.seuprojeto.pascoa.notificacao.service.AlertaInternoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Collections;
import java.util.List;

/**
 * Injeta dados globais (alertas, badge) no model de todas as páginas autenticadas.
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

    private boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal());
    }
}
