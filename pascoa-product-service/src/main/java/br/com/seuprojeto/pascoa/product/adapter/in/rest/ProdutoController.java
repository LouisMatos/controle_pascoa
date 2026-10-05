package br.com.seuprojeto.pascoa.product.adapter.in.rest;

import br.com.seuprojeto.pascoa.product.adapter.in.rest.dto.ProdutoRequest;
import br.com.seuprojeto.pascoa.product.adapter.in.rest.dto.ProdutoResponse;
import br.com.seuprojeto.pascoa.product.application.port.in.ProdutoUseCase;
import br.com.seuprojeto.pascoa.product.domain.exception.PrecoInvalidoException;
import br.com.seuprojeto.pascoa.product.domain.exception.ProdutoNotFoundException;
import br.com.seuprojeto.pascoa.product.domain.model.Categoria;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/produtos")
@RequiredArgsConstructor
public class ProdutoController {

    private final ProdutoUseCase produtoUseCase;

    // ── Endpoints autenticados ─────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE','CONFEITEIRO','ANALISTA','FINANCEIRO')")
    public List<ProdutoResponse> listar() {
        return produtoUseCase.listar().stream().map(ProdutoResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE','CONFEITEIRO','ANALISTA','FINANCEIRO')")
    public ProdutoResponse buscar(@PathVariable Long id) {
        return ProdutoResponse.from(produtoUseCase.buscarPorId(id));
    }

    @GetMapping("/categoria/{categoria}")
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE','CONFEITEIRO','ANALISTA')")
    public List<ProdutoResponse> porCategoria(@PathVariable Categoria categoria) {
        return produtoUseCase.listarPorCategoria(categoria).stream()
                .map(ProdutoResponse::from).toList();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ProdutoResponse> criar(@Valid @RequestBody ProdutoRequest req) {
        var p = produtoUseCase.criar(new ProdutoUseCase.CriarProdutoCommand(
                req.nome(), req.descricao(), req.preco(), req.categoria(), req.fotoUrl()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ProdutoResponse.from(p));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ProdutoResponse atualizar(@PathVariable Long id,
                                     @Valid @RequestBody ProdutoRequest req,
                                     @RequestParam(defaultValue = "true") boolean disponivel) {
        var p = produtoUseCase.atualizar(new ProdutoUseCase.AtualizarProdutoCommand(
                id, req.nome(), req.descricao(), req.preco(),
                req.categoria(), req.fotoUrl(), disponivel));
        return ProdutoResponse.from(p);
    }

    @PatchMapping("/{id}/foto")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ProdutoResponse atualizarFoto(@PathVariable Long id,
                                         @RequestParam String fotoUrl) {
        return ProdutoResponse.from(produtoUseCase.atualizarFoto(id, fotoUrl));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        produtoUseCase.inativar(id);
        return ResponseEntity.noContent().build();
    }

    // ── Catálogo público (sem autenticação) ───────────────────────────────────

    @GetMapping("/catalogo")
    public List<ProdutoResponse> catalogo() {
        return produtoUseCase.listarDisponiveis().stream().map(ProdutoResponse::from).toList();
    }

    @GetMapping("/catalogo/categoria/{categoria}")
    public List<ProdutoResponse> catalogoPorCategoria(@PathVariable Categoria categoria) {
        return produtoUseCase.listarPorCategoria(categoria).stream()
                .filter(p -> p.isDisponivel() && p.isAtivo())
                .map(ProdutoResponse::from).toList();
    }

    // ── Exception handlers ────────────────────────────────────────────────────

    @ExceptionHandler(ProdutoNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ProdutoNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(PrecoInvalidoException.class)
    public ResponseEntity<Map<String, String>> handlePreco(PrecoInvalidoException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
    }
}
