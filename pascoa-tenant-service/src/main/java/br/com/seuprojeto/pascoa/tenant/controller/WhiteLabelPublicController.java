package br.com.seuprojeto.pascoa.tenant.controller;

import br.com.seuprojeto.pascoa.tenant.domain.WhiteLabelConfig;
import br.com.seuprojeto.pascoa.tenant.repository.WhiteLabelConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * v6 Etapa 13 — Endpoints PÚBLICOS de white-label.
 *
 * <p>Consumidos por PWA / browser do cliente final — sem autenticação, com
 * cache HTTP de 5 minutos (mudanças visuais não precisam ser instantâneas).
 * Devem ser expostos no API Gateway como bypass de auth/rate-limit em prod.
 */
@RestController
@RequestMapping("/tenants/{tenantId}")
@RequiredArgsConstructor
public class WhiteLabelPublicController {

    private static final CacheControl CACHE = CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic();

    private final WhiteLabelConfigRepository repo;

    /** PWA manifest.json gerado com nome, cores e ícones do tenant. */
    @GetMapping(value = "/manifest.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> manifest(@PathVariable String tenantId) {
        WhiteLabelConfig cfg = repo.findById(tenantId).orElse(defaultConfig(tenantId));

        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("name",            valor(cfg.getNomeApp(),       "FoodFlow"));
        manifest.put("short_name",      valor(cfg.getNomeApp(),       "FoodFlow"));
        manifest.put("description",     "App de gestão — " + valor(cfg.getNomeApp(), "FoodFlow"));
        manifest.put("start_url",       "/");
        manifest.put("display",         "standalone");
        manifest.put("background_color", valor(cfg.getCorSecundaria(), "#ffffff"));
        manifest.put("theme_color",      valor(cfg.getCorPrimaria(),   "#1976d2"));
        if (cfg.getLogoUrl() != null && !cfg.getLogoUrl().isBlank()) {
            manifest.put("icons", List.of(
                    Map.of("src", cfg.getLogoUrl(), "sizes", "192x192", "type", "image/png"),
                    Map.of("src", cfg.getLogoUrl(), "sizes", "512x512", "type", "image/png")));
        }
        return ResponseEntity.ok().cacheControl(CACHE).body(manifest);
    }

    /** CSS dinâmico com variáveis do tenant — basta importar no <link rel="stylesheet"> da PWA. */
    @GetMapping(value = "/theme.css", produces = "text/css")
    public ResponseEntity<String> themeCss(@PathVariable String tenantId) {
        WhiteLabelConfig cfg = repo.findById(tenantId).orElse(defaultConfig(tenantId));
        String css = """
                /* FoodFlow v6 — tema gerado para tenant=%s */
                :root {
                    --foodflow-cor-primaria:   %s;
                    --foodflow-cor-secundaria: %s;
                    --foodflow-nome-app:       "%s";
                }
                body { background-color: var(--foodflow-cor-secundaria); }
                .navbar, .btn-primary, .badge-primary { background-color: var(--foodflow-cor-primaria) !important; }
                a, .text-primary { color: var(--foodflow-cor-primaria); }
                """.formatted(
                        tenantId,
                        valor(cfg.getCorPrimaria(),   "#1976d2"),
                        valor(cfg.getCorSecundaria(), "#ffffff"),
                        valor(cfg.getNomeApp(),       "FoodFlow").replace("\"", "\\\""));
        return ResponseEntity.ok().cacheControl(CACHE).body(css);
    }

    /** White-label JSON consumido pelo {@code WhiteLabelClient} dos microsserviços. */
    @GetMapping(value = "/white-label", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WhiteLabelConfig> whiteLabel(@PathVariable String tenantId) {
        WhiteLabelConfig cfg = repo.findById(tenantId).orElse(defaultConfig(tenantId));
        return ResponseEntity.ok().cacheControl(CACHE).body(cfg);
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private static String valor(String v, String fallback) {
        return (v == null || v.isBlank()) ? fallback : v;
    }

    private static WhiteLabelConfig defaultConfig(String tenantId) {
        return WhiteLabelConfig.builder()
                .tenantId(tenantId)
                .nomeApp("FoodFlow")
                .corPrimaria("#1976d2")
                .corSecundaria("#ffffff")
                .build();
    }
}
