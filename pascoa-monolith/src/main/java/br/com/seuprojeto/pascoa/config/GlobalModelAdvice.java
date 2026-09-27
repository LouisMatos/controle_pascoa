package br.com.seuprojeto.pascoa.config;

import br.com.seuprojeto.pascoa.notificacao.entity.AlertaInterno;
import br.com.seuprojeto.pascoa.notificacao.service.AlertaInternoService;
import br.com.seuprojeto.pascoa.shared.SecurityUtils;
import lombok.RequiredArgsConstructor;
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
        if (!SecurityUtils.autenticado()) return Collections.emptyList();
        try {
            return alertaService.recentes();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @ModelAttribute("alertasNaoLidos")
    public long alertasNaoLidos() {
        if (!SecurityUtils.autenticado()) return 0L;
        try {
            return alertaService.contarNaoLidos();
        } catch (Exception e) {
            return 0L;
        }
    }

}
