package br.com.seuprojeto.pascoa.configengine.controller;

import br.com.seuprojeto.pascoa.configengine.domain.TipoNegocio;
import br.com.seuprojeto.pascoa.configengine.dto.ConfigEfetivaResponse;
import br.com.seuprojeto.pascoa.configengine.exception.ConfigException;
import br.com.seuprojeto.pascoa.configengine.service.ConfigEngineService;
import br.com.seuprojeto.pascoa.configengine.template.TemplateNegocio.AtributoProduto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/config")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigEngineService service;

    @PostMapping("/{tenantId}/aplicar-template/{tipo}")
    public ConfigEfetivaResponse aplicarTemplate(@PathVariable String tenantId,
                                                 @PathVariable TipoNegocio tipo) {
        service.aplicarTemplate(tenantId, tipo);
        return service.buscarEfetiva(tenantId);
    }

    @GetMapping("/{tenantId}/efetivo")
    public ConfigEfetivaResponse efetiva(@PathVariable String tenantId) {
        return service.buscarEfetiva(tenantId);
    }

    @GetMapping("/{tenantId}/categorias")
    public List<String> categorias(@PathVariable String tenantId) {
        return service.categorias(tenantId);
    }

    @PostMapping("/{tenantId}/categorias")
    public List<String> adicionarCategoria(@PathVariable String tenantId,
                                           @RequestBody Map<String, String> body) {
        return service.adicionarCategoria(tenantId, body.get("nome"));
    }

    @GetMapping("/{tenantId}/fases-producao")
    public List<String> fases(@PathVariable String tenantId) {
        return service.fasesProducao(tenantId);
    }

    @PutMapping("/{tenantId}/fases-producao")
    public List<String> substituirFases(@PathVariable String tenantId,
                                        @RequestBody Map<String, List<String>> body) {
        return service.substituirFasesProducao(tenantId, body.get("fases"));
    }

    @GetMapping("/{tenantId}/atributos-produto")
    public List<AtributoProduto> atributos(@PathVariable String tenantId) {
        return service.atributosProduto(tenantId);
    }

    // ── exceptions ───────────────────────────────────────────────────────

    @ExceptionHandler(ConfigException.class)
    public ResponseEntity<Map<String, String>> handle(ConfigException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }
}
