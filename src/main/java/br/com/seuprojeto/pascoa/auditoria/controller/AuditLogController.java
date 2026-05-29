package br.com.seuprojeto.pascoa.auditoria.controller;

import br.com.seuprojeto.pascoa.auditoria.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/auditoria")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService service;

    @GetMapping
    public String lista(@RequestParam(required = false) String usuario,
                        @RequestParam(required = false) String acao,
                        @RequestParam(defaultValue = "0") int pagina,
                        Model model) {
        var page = service.listar(usuario, acao, pagina);
        model.addAttribute("logs",    page.getContent());
        model.addAttribute("page",    page);
        model.addAttribute("usuario", usuario);
        model.addAttribute("acao",    acao);
        model.addAttribute("pagina",  pagina);
        return "auditoria/lista";
    }
}
