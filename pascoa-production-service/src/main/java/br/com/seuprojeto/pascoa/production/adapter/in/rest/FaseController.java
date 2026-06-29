package br.com.seuprojeto.pascoa.production.adapter.in.rest;

import br.com.seuprojeto.pascoa.production.adapter.in.rest.dto.FaseRequest;
import br.com.seuprojeto.pascoa.production.adapter.in.rest.dto.HistoricoFaseResponse;
import br.com.seuprojeto.pascoa.production.adapter.out.persistence.OrdemProducaoJpaEntity;
import br.com.seuprojeto.pascoa.production.application.fases.FaseService;
import br.com.seuprojeto.pascoa.production.domain.exception.OrdemNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ordens")
@RequiredArgsConstructor
public class FaseController {

    private final FaseService faseService;

    /**
     * Transiciona a ordem para a fase informada (nome + ordem dentro do fluxo do tenant).
     * Cliente é responsável por enviar o nome exato conforme {@code config-engine}.
     */
    @PostMapping("/{ordemId}/fases")
    public Map<String, Object> transicionar(@PathVariable Long ordemId,
                                            @Valid @RequestBody FaseRequest req) {
        OrdemProducaoJpaEntity ordem = faseService.transicionar(ordemId, req);
        return Map.of(
                "ordemId",    ordem.getId(),
                "faseAtual",  ordem.getFaseAtual(),
                "faseOrdem",  ordem.getFaseOrdem(),
                "status",     ordem.getStatus(),
                "iniciadaEm", ordem.getIniciadaEm()
        );
    }

    @GetMapping("/{ordemId}/fases/historico")
    public List<HistoricoFaseResponse> historico(@PathVariable Long ordemId) {
        return faseService.historico(ordemId).stream().map(HistoricoFaseResponse::from).toList();
    }

    @ExceptionHandler(OrdemNotFoundException.class)
    public ResponseEntity<Map<String, String>> notFound(OrdemNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }
}
