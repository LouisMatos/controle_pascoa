package br.com.seuprojeto.pascoa.tenant.controller;

import br.com.seuprojeto.pascoa.tenant.dto.OnboardingRequest;
import br.com.seuprojeto.pascoa.tenant.dto.OnboardingResponse;
import br.com.seuprojeto.pascoa.tenant.service.OnboardingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/onboarding")
@RequiredArgsConstructor
@Tag(name = "Onboarding", description = "Wizard self-service de criação de tenant SaaS")
public class OnboardingController {

    private final OnboardingService onboarding;

    /**
     * Endpoint principal do wizard. Orquestra registro + provisionamento + template + trial
     * + white-label em uma chamada idempotente-friendly.
     *
     * <p>Retorna 201 + diagnóstico por etapa; 207 se alguma etapa best-effort falhou
     * (tenant existe mas precisa ser completado pelo painel admin).
     */
    @PostMapping("/iniciar")
    public ResponseEntity<OnboardingResponse> iniciar(@Valid @RequestBody OnboardingRequest req) {
        OnboardingResponse out = onboarding.executar(req);
        HttpStatus status = out.sucesso() ? HttpStatus.CREATED : HttpStatus.MULTI_STATUS;
        return ResponseEntity.status(status).body(out);
    }
}
