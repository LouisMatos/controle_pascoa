package br.com.seuprojeto.pascoa.pricing.controller;

import br.com.seuprojeto.pascoa.pricing.domain.ModeloPreco;
import br.com.seuprojeto.pascoa.pricing.domain.PricingRequest;
import br.com.seuprojeto.pascoa.pricing.domain.PricingResponse;
import br.com.seuprojeto.pascoa.pricing.exception.PricingException;
import br.com.seuprojeto.pascoa.pricing.service.PricingEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/pricing")
@RequiredArgsConstructor
public class PricingController {

    private final PricingEngine engine;

    @PostMapping("/calcular")
    public PricingResponse calcular(@RequestBody PricingRequest request) {
        return engine.calcular(request);
    }

    @GetMapping("/modelos")
    public List<ModeloPreco> modelos() {
        return List.of(ModeloPreco.values());
    }

    @ExceptionHandler(PricingException.class)
    public ResponseEntity<Map<String, String>> handle(PricingException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("error", ex.getMessage()));
    }
}
