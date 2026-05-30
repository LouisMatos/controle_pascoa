package br.com.seuprojeto.pascoa.production.adapter.in.rest;

import br.com.seuprojeto.pascoa.production.adapter.in.rest.dto.OrdemResponse;
import br.com.seuprojeto.pascoa.production.application.port.in.ProducaoUseCase;
import br.com.seuprojeto.pascoa.production.domain.exception.OrdemNotFoundException;
import br.com.seuprojeto.pascoa.production.domain.exception.TransicaoOrdemInvalidaException;
import br.com.seuprojeto.pascoa.production.domain.model.StatusOrdem;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/producao")
@RequiredArgsConstructor
public class ProducaoController {

    private final ProducaoUseCase producaoUseCase;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CONFEITEIRO','ANALISTA')")
    public List<OrdemResponse> listar() {
        return producaoUseCase.listar().stream().map(OrdemResponse::from).toList();
    }

    @GetMapping("/kanban")
    @PreAuthorize("hasAnyRole('ADMIN','CONFEITEIRO')")
    public Map<String, List<OrdemResponse>> kanban() {
        return Map.of(
                "PENDENTE", producaoUseCase.listarPorStatus(StatusOrdem.PENDENTE).stream()
                        .map(OrdemResponse::from).toList(),
                "EM_ANDAMENTO", producaoUseCase.listarPorStatus(StatusOrdem.EM_ANDAMENTO).stream()
                        .map(OrdemResponse::from).toList(),
                "CONCLUIDA", producaoUseCase.listarPorStatus(StatusOrdem.CONCLUIDA).stream()
                        .map(OrdemResponse::from).toList()
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CONFEITEIRO','ANALISTA')")
    public OrdemResponse buscar(@PathVariable Long id) {
        return OrdemResponse.from(producaoUseCase.buscarPorId(id));
    }

    @GetMapping("/pedido/{pedidoId}")
    @PreAuthorize("hasAnyRole('ADMIN','CONFEITEIRO','ANALISTA')")
    public OrdemResponse buscarPorPedido(@PathVariable Long pedidoId) {
        return OrdemResponse.from(producaoUseCase.buscarPorPedidoId(pedidoId));
    }

    @PostMapping("/{id}/iniciar")
    @PreAuthorize("hasAnyRole('ADMIN','CONFEITEIRO')")
    public OrdemResponse iniciar(@PathVariable Long id) {
        return OrdemResponse.from(producaoUseCase.iniciar(id));
    }

    @PostMapping("/{id}/concluir")
    @PreAuthorize("hasAnyRole('ADMIN','CONFEITEIRO')")
    public OrdemResponse concluir(@PathVariable Long id) {
        return OrdemResponse.from(producaoUseCase.concluir(id));
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public OrdemResponse cancelar(@PathVariable Long id) {
        return OrdemResponse.from(producaoUseCase.cancelar(id));
    }

    @ExceptionHandler(OrdemNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(OrdemNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(TransicaoOrdemInvalidaException.class)
    public ResponseEntity<Map<String, String>> handleTransicao(TransicaoOrdemInvalidaException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("error", e.getMessage()));
    }
}
