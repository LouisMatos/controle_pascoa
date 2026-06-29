package br.com.seuprojeto.pascoa.tenant.controller;

import br.com.seuprojeto.pascoa.tenant.domain.WhiteLabelConfig;
import br.com.seuprojeto.pascoa.tenant.dto.RegistrarTenantRequest;
import br.com.seuprojeto.pascoa.tenant.dto.TenantResponse;
import br.com.seuprojeto.pascoa.tenant.dto.WhiteLabelRequest;
import br.com.seuprojeto.pascoa.tenant.security.RegistrarRateLimiter;
import br.com.seuprojeto.pascoa.tenant.service.TenantService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/tenants")
@RequiredArgsConstructor
@Tag(name = "Tenants", description = "Registro, ciclo de vida e white-label dos tenants FoodFlow")
public class TenantController {

    private final TenantService tenantService;
    private final RegistrarRateLimiter rateLimiter;

    @PostMapping("/registrar")
    public ResponseEntity<?> registrar(@Valid @RequestBody RegistrarTenantRequest req,
                                       HttpServletRequest httpReq) {
        // S-03 — rate limit por IP. Honra X-Forwarded-For quando atrás de proxy/gateway.
        if (!rateLimiter.permitir(resolveClientIp(httpReq))) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Limite de registros excedido. Tente novamente em 1 hora."));
        }
        var tenant = tenantService.registrar(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(TenantResponse.from(tenant));
    }

    private String resolveClientIp(HttpServletRequest req) {
        String fwd = req.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) {
            int comma = fwd.indexOf(',');
            return (comma > 0 ? fwd.substring(0, comma) : fwd).trim();
        }
        return req.getRemoteAddr();
    }

    @GetMapping("/{id}")
    public TenantResponse buscar(@PathVariable String id) {
        return TenantResponse.from(tenantService.buscarPorId(id));
    }

    @PutMapping("/{id}/white-label")
    public WhiteLabelConfig atualizarWhiteLabel(@PathVariable String id,
                                                @Valid @RequestBody WhiteLabelRequest req) {
        return tenantService.atualizarWhiteLabel(id, req);
    }

    @PutMapping("/{id}/dominio")
    public TenantResponse atualizarDominio(@PathVariable String id,
                                           @RequestBody Map<@NotBlank String, @NotBlank String> body) {
        String dominio = body.get("dominioCustom");
        return TenantResponse.from(tenantService.atualizarDominio(id, dominio));
    }

    @PostMapping("/{id}/suspender")
    public TenantResponse suspender(@PathVariable String id) {
        return TenantResponse.from(tenantService.suspender(id));
    }

    @PostMapping("/{id}/reativar")
    public TenantResponse reativar(@PathVariable String id) {
        return TenantResponse.from(tenantService.reativar(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelar(@PathVariable String id) {
        tenantService.cancelar(id);
    }
}
