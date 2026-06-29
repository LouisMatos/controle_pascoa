package br.com.seuprojeto.pascoa.notification.adapter.in.rest;

import br.com.seuprojeto.pascoa.notification.adapter.in.rest.dto.RenderRequest;
import br.com.seuprojeto.pascoa.notification.adapter.in.rest.dto.RenderResponse;
import br.com.seuprojeto.pascoa.notification.adapter.in.rest.dto.TemplateRequest;
import br.com.seuprojeto.pascoa.notification.adapter.out.persistence.NotifTemplateJpaEntity;
import br.com.seuprojeto.pascoa.notification.application.template.TemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NotifTemplateJpaEntity criar(@Valid @RequestBody TemplateRequest req) {
        return service.criar(req);
    }

    @GetMapping("/tenants/{tenantId}")
    public List<NotifTemplateJpaEntity> listarDoTenant(@PathVariable String tenantId) {
        return service.listarDoTenant(tenantId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desativar(@PathVariable Long id) {
        service.desativar(id);
    }

    /** Resolve template (com fallback global) + renderiza variáveis. */
    @PostMapping("/render")
    public RenderResponse renderizar(@Valid @RequestBody RenderRequest req) {
        return service.renderizar(req.tenantId(), req.evento(), req.canal(),
                req.variaveis() != null ? req.variaveis() : Map.of());
    }

    @ExceptionHandler(TemplateService.TemplateNotFoundException.class)
    public ResponseEntity<Map<String, String>> notFound(TemplateService.TemplateNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }
}
