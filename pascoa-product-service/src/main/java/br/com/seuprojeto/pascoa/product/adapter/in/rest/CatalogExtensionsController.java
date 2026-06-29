package br.com.seuprojeto.pascoa.product.adapter.in.rest;

import br.com.seuprojeto.pascoa.product.adapter.in.rest.dto.*;
import br.com.seuprojeto.pascoa.product.adapter.out.persistence.ProdutoFichaNutricionalJpaEntity;
import br.com.seuprojeto.pascoa.product.application.catalog.CatalogExtensionsService;
import br.com.seuprojeto.pascoa.product.domain.exception.ProdutoNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/produtos")
@RequiredArgsConstructor
public class CatalogExtensionsController {

    private final CatalogExtensionsService service;

    // ── Atributos dinâmicos ──────────────────────────────────────────────

    @GetMapping("/{produtoId}/atributos")
    public Map<String, String> listarAtributos(@PathVariable Long produtoId) {
        return service.listarAtributos(produtoId);
    }

    @PutMapping("/{produtoId}/atributos")
    public Map<String, String> definirAtributo(@PathVariable Long produtoId,
                                               @Valid @RequestBody AtributoRequest req) {
        return service.definirAtributo(produtoId, req);
    }

    @DeleteMapping("/{produtoId}/atributos/{chave}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerAtributo(@PathVariable Long produtoId, @PathVariable String chave) {
        service.removerAtributo(produtoId, chave);
    }

    // ── Variantes ────────────────────────────────────────────────────────

    @GetMapping("/{produtoId}/variantes")
    public List<VarianteResponse> listarVariantes(@PathVariable Long produtoId) {
        return service.listarVariantes(produtoId);
    }

    @PostMapping("/{produtoId}/variantes")
    @ResponseStatus(HttpStatus.CREATED)
    public VarianteResponse criarVariante(@PathVariable Long produtoId,
                                          @Valid @RequestBody VarianteRequest req) {
        return service.criarVariante(produtoId, req);
    }

    @PutMapping("/variantes/{varianteId}")
    public VarianteResponse atualizarVariante(@PathVariable Long varianteId,
                                              @Valid @RequestBody VarianteRequest req) {
        return service.atualizarVariante(varianteId, req);
    }

    @DeleteMapping("/variantes/{varianteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerVariante(@PathVariable Long varianteId) {
        service.removerVariante(varianteId);
    }

    // ── Ficha nutricional ───────────────────────────────────────────────

    @GetMapping("/{produtoId}/ficha-nutricional")
    public ResponseEntity<ProdutoFichaNutricionalJpaEntity> buscarFicha(@PathVariable Long produtoId) {
        var ficha = service.buscarFicha(produtoId);
        return ficha != null ? ResponseEntity.ok(ficha) : ResponseEntity.noContent().build();
    }

    @PutMapping("/{produtoId}/ficha-nutricional")
    public ProdutoFichaNutricionalJpaEntity salvarFicha(@PathVariable Long produtoId,
                                                       @Valid @RequestBody FichaNutricionalRequest req) {
        return service.salvarFicha(produtoId, req);
    }

    // ── error handler ────────────────────────────────────────────────────

    @ExceptionHandler(ProdutoNotFoundException.class)
    public ResponseEntity<Map<String, String>> notFound(ProdutoNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }
}
