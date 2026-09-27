package br.com.seuprojeto.pascoa.inventory.adapter.in.rest;

import br.com.seuprojeto.pascoa.inventory.adapter.in.rest.dto.MateriaPrimaRequest;
import br.com.seuprojeto.pascoa.inventory.adapter.in.rest.dto.MateriaPrimaResponse;
import br.com.seuprojeto.pascoa.inventory.adapter.in.rest.dto.MovimentacaoRequest;
import br.com.seuprojeto.pascoa.inventory.application.port.in.EstoqueUseCase;
import br.com.seuprojeto.pascoa.inventory.domain.exception.EstoqueInsuficienteException;
import br.com.seuprojeto.pascoa.inventory.domain.exception.MateriaPrimaNotFoundException;
import br.com.seuprojeto.pascoa.inventory.domain.model.Movimentacao;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/materias-primas")
@RequiredArgsConstructor
public class InventarioController {

    private final EstoqueUseCase estoqueUseCase;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CONFEITEIRO','ANALISTA','FINANCEIRO')")
    public List<MateriaPrimaResponse> listar() {
        return estoqueUseCase.listar().stream().map(MateriaPrimaResponse::from).toList();
    }

    @GetMapping("/criticos")
    @PreAuthorize("hasAnyRole('ADMIN','CONFEITEIRO','ANALISTA')")
    public List<MateriaPrimaResponse> listarCriticos() {
        return estoqueUseCase.listarCriticos().stream().map(MateriaPrimaResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CONFEITEIRO','ANALISTA','FINANCEIRO')")
    public MateriaPrimaResponse buscar(@PathVariable Long id) {
        return MateriaPrimaResponse.from(estoqueUseCase.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CONFEITEIRO')")
    public ResponseEntity<MateriaPrimaResponse> criar(@Valid @RequestBody MateriaPrimaRequest req) {
        var mp = estoqueUseCase.criar(new EstoqueUseCase.CriarMateriaPrimaCommand(
                req.nome(), req.unidade(), req.estoqueInicial(), req.estoqueMinimo(), req.fornecedorId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(MateriaPrimaResponse.from(mp));
    }

    @PostMapping("/{id}/entrada")
    @PreAuthorize("hasAnyRole('ADMIN','CONFEITEIRO')")
    public Movimentacao entrada(@PathVariable Long id,
                                @Valid @RequestBody MovimentacaoRequest req) {
        return estoqueUseCase.registrarEntrada(
                new EstoqueUseCase.MovimentarEstoqueCommand(id, req.quantidade(), req.observacao()));
    }

    @PostMapping("/{id}/saida")
    @PreAuthorize("hasAnyRole('ADMIN','CONFEITEIRO')")
    public Movimentacao saida(@PathVariable Long id,
                              @Valid @RequestBody MovimentacaoRequest req) {
        return estoqueUseCase.registrarSaida(
                new EstoqueUseCase.MovimentarEstoqueCommand(id, req.quantidade(), req.observacao()));
    }

    @GetMapping("/{id}/movimentacoes")
    @PreAuthorize("hasAnyRole('ADMIN','CONFEITEIRO','ANALISTA')")
    public List<Movimentacao> movimentacoes(@PathVariable Long id) {
        return estoqueUseCase.listarMovimentacoes(id);
    }

    @GetMapping("/{id}/disponibilidade")
    @PreAuthorize("hasAnyRole('ADMIN','CONFEITEIRO')")
    public Map<String, Object> verificarDisponibilidade(@PathVariable Long id,
                                                        @RequestParam java.math.BigDecimal quantidade) {
        boolean disponivel = estoqueUseCase.verificarDisponibilidade(id, quantidade);
        return Map.of("materiaPrimaId", id, "quantidade", quantidade, "disponivel", disponivel);
    }

    @ExceptionHandler(MateriaPrimaNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(MateriaPrimaNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(EstoqueInsuficienteException.class)
    public ResponseEntity<Map<String, String>> handleInsuficiente(EstoqueInsuficienteException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("error", e.getMessage()));
    }
}
