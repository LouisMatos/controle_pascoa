package br.com.seuprojeto.pascoa.tenant.controller;

import br.com.seuprojeto.pascoa.tenant.dto.ApiKeyRequest;
import br.com.seuprojeto.pascoa.tenant.dto.ApiKeyResponse;
import br.com.seuprojeto.pascoa.tenant.service.ApiKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tenants/{tenantId}/api-keys")
@RequiredArgsConstructor
@Tag(name = "API Keys", description = "Gestão de chaves para integração externa (plano ENTERPRISE)")
public class ApiKeyController {

    private final ApiKeyService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Cria uma nova chave de API",
            description = """
                    Gera uma chave plain-text aleatória (256 bits, prefixo `ff_live_`) e
                    persiste APENAS o hash SHA-256. **O campo `chavePlain` na resposta
                    é a única oportunidade de copiar o valor.** Subsequentes leituras
                    retornam apenas `chavePrefix` para identificação visual.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Chave criada com sucesso (plain-text incluso)"),
            @ApiResponse(responseCode = "404", description = "Tenant inexistente"),
            @ApiResponse(responseCode = "400", description = "Payload inválido")
    })
    public ApiKeyResponse criar(@PathVariable String tenantId,
                                @Valid @RequestBody ApiKeyRequest req) {
        var resultado = service.criar(tenantId, req.nome(), req.escopos(), req.expiraEm());
        return ApiKeyResponse.from(resultado.metadata(), resultado.plainKey());
    }

    @GetMapping
    @Operation(summary = "Lista chaves do tenant (sem plain-text)")
    public List<ApiKeyResponse> listar(@PathVariable String tenantId) {
        return service.listar(tenantId).stream().map(ApiKeyResponse::from).toList();
    }

    @DeleteMapping("/{keyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Revoga uma chave",
            description = "Soft-delete via `ativa = false`. Validações subsequentes falham imediatamente.")
    public void revogar(@PathVariable String tenantId, @PathVariable Long keyId) {
        service.revogar(keyId);
    }
}
