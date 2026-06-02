package br.com.seuprojeto.pascoa.config;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * V9 Etapa 1.6 — Resolve códigos de erro para templates autossuficientes em
 * {@code templates/error/} (500/404/403). Não depende de layout nem de tenant,
 * portanto continua funcionando mesmo quando o template principal está quebrado.
 */
@Controller
public class AppErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Integer statusCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object messageAttr  = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        if (messageAttr != null) {
            model.addAttribute("message", messageAttr.toString());
        }
        if (statusCode != null) {
            if (statusCode == 404) return "error/404";
            if (statusCode == 403) return "error/403";
        }
        return "error/500";
    }
}
