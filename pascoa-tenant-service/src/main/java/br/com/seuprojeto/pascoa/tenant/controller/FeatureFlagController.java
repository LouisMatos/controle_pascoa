package br.com.seuprojeto.pascoa.tenant.controller;

import br.com.seuprojeto.pascoa.tenant.service.FeatureFlagService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/tenants/{tenantId}/feature-flags")
@RequiredArgsConstructor
@Tag(name = "Feature Flags", description = "Toggles por tenant — override > default do plano")
public class FeatureFlagController {

    private final FeatureFlagService featureFlagService;

    @GetMapping
    public Map<String, Boolean> listar(@PathVariable String tenantId) {
        return featureFlagService.resolverTodas(tenantId);
    }

    @GetMapping("/{chave}")
    public Map<String, Object> consultar(@PathVariable String tenantId, @PathVariable String chave) {
        return Map.of("chave", chave, "ativo", featureFlagService.isAtivo(tenantId, chave));
    }

    @PutMapping("/{chave}")
    public Map<String, Object> definir(@PathVariable String tenantId,
                                       @PathVariable String chave,
                                       @RequestBody Map<String, Boolean> body) {
        boolean ativo = Boolean.TRUE.equals(body.get("ativo"));
        featureFlagService.definir(tenantId, chave, ativo);
        return Map.of("chave", chave, "ativo", ativo);
    }

    @DeleteMapping("/{chave}")
    public Map<String, Object> remover(@PathVariable String tenantId, @PathVariable String chave) {
        featureFlagService.remover(tenantId, chave);
        return Map.of("chave", chave, "ativo", featureFlagService.isAtivo(tenantId, chave));
    }
}
