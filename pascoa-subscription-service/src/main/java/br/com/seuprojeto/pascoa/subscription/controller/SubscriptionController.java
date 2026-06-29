package br.com.seuprojeto.pascoa.subscription.controller;

import br.com.seuprojeto.pascoa.subscription.domain.*;
import br.com.seuprojeto.pascoa.subscription.exception.SubscriptionException;
import br.com.seuprojeto.pascoa.subscription.service.MeteringService;
import br.com.seuprojeto.pascoa.subscription.service.SubscriptionService;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final MeteringService metering;

    @PostMapping("/iniciar-trial")
    @ResponseStatus(HttpStatus.CREATED)
    public Assinatura iniciarTrial(@RequestBody Map<@NotBlank String, @NotBlank String> body) {
        return subscriptionService.iniciarTrial(body.get("tenantId"), body.get("email"));
    }

    @PostMapping("/{tenantId}/upgrade")
    public Assinatura upgrade(@PathVariable String tenantId,
                              @RequestBody Map<String, String> body) {
        Plano plano = Plano.valueOf(body.get("plano"));
        return subscriptionService.upgrade(tenantId, plano);
    }

    @PostMapping("/{tenantId}/cancelar")
    public Assinatura cancelar(@PathVariable String tenantId) {
        return subscriptionService.cancelar(tenantId);
    }

    @GetMapping("/{tenantId}")
    public Assinatura buscar(@PathVariable String tenantId) {
        return subscriptionService.buscarPorTenant(tenantId);
    }

    @GetMapping("/{tenantId}/limites")
    public LimitesPlano limites(@PathVariable String tenantId) {
        return subscriptionService.limitesDoTenant(tenantId);
    }

    @GetMapping("/{tenantId}/uso")
    public UsoMetrico usoCorrente(@PathVariable String tenantId) {
        return metering.usoCorrente(tenantId);
    }

    @GetMapping("/{tenantId}/uso/historico")
    public List<UsoMetrico> usoHistorico(@PathVariable String tenantId) {
        return metering.historico(tenantId);
    }

    /** Incrementa contador de uso. Chamado por outros microsserviços em eventos. */
    @PostMapping("/{tenantId}/uso/{metrica}")
    public UsoMetrico incrementar(@PathVariable String tenantId,
                                  @PathVariable MeteringService.Metrica metrica,
                                  @RequestBody Map<String, Long> body) {
        long delta = body.getOrDefault("delta", 1L);
        return metering.incrementar(tenantId, metrica, delta);
    }

    @ExceptionHandler(SubscriptionException.class)
    public ResponseEntity<Map<String, String>> handle(SubscriptionException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("error", ex.getMessage()));
    }
}
