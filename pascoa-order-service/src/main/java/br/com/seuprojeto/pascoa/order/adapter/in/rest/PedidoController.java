package br.com.seuprojeto.pascoa.order.adapter.in.rest;

import br.com.seuprojeto.pascoa.order.adapter.in.rest.dto.*;
import br.com.seuprojeto.pascoa.order.application.port.in.PedidoUseCase;
import br.com.seuprojeto.pascoa.order.domain.exception.PedidoNotFoundException;
import br.com.seuprojeto.pascoa.order.domain.exception.TransicaoInvalidaException;
import br.com.seuprojeto.pascoa.order.domain.model.FormaPagamento;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoUseCase pedidoUseCase;

    // ── CRUD autenticado ───────────────────────────────────────────────────────

    @GetMapping("/api/pedidos")
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE','ANALISTA','FINANCEIRO')")
    public List<PedidoResponse> listar() {
        return pedidoUseCase.listar().stream().map(PedidoResponse::from).toList();
    }

    @GetMapping("/api/pedidos/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE','ANALISTA','FINANCEIRO')")
    public PedidoResponse buscar(@PathVariable Long id) {
        return PedidoResponse.from(pedidoUseCase.buscarPorId(id));
    }

    @PostMapping("/api/pedidos")
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
    public ResponseEntity<PedidoResponse> criar(@Valid @RequestBody CriarPedidoRequest req) {
        var pedido = pedidoUseCase.criar(new PedidoUseCase.CriarPedidoCommand(req.clienteId(), req.observacao()));
        return ResponseEntity.status(HttpStatus.CREATED).body(PedidoResponse.from(pedido));
    }

    @PostMapping("/api/pedidos/{id}/itens")
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
    public PedidoResponse adicionarItem(@PathVariable Long id,
                                        @Valid @RequestBody AdicionarItemRequest req) {
        return PedidoResponse.from(pedidoUseCase.adicionarItem(
                new PedidoUseCase.AdicionarItemCommand(id, req.produtoId(), req.quantidade())));
    }

    @DeleteMapping("/api/pedidos/{id}/itens/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
    public PedidoResponse removerItem(@PathVariable Long id, @PathVariable Long itemId) {
        return PedidoResponse.from(pedidoUseCase.removerItem(id, itemId));
    }

    @PostMapping("/api/pedidos/{id}/confirmar")
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
    public PedidoResponse confirmar(@PathVariable Long id) {
        return PedidoResponse.from(pedidoUseCase.confirmar(id));
    }

    @PostMapping("/api/pedidos/{id}/cancelar")
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
    public PedidoResponse cancelar(@PathVariable Long id) {
        return PedidoResponse.from(pedidoUseCase.cancelar(id));
    }

    @PostMapping("/api/pedidos/{id}/pronto")
    @PreAuthorize("hasAnyRole('ADMIN','CONFEITEIRO')")
    public PedidoResponse marcarPronto(@PathVariable Long id) {
        return PedidoResponse.from(pedidoUseCase.marcarPronto(id));
    }

    @PostMapping("/api/pedidos/{id}/entregar")
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
    public PedidoResponse registrarEntrega(@PathVariable Long id) {
        return PedidoResponse.from(pedidoUseCase.registrarEntrega(id));
    }

    @PostMapping("/api/pedidos/{id}/pagamento")
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
    public PedidoResponse registrarPagamento(@PathVariable Long id,
                                             @RequestParam FormaPagamento forma,
                                             @RequestParam(required = false) BigDecimal valor) {
        return PedidoResponse.from(pedidoUseCase.registrarPagamento(
                new PedidoUseCase.RegistrarPagamentoCommand(id, forma, valor)));
    }

    // ── Rastreamento público ────────────────────────────────────────────────────

    @GetMapping("/api/acompanhamento/{token}")
    public PedidoResponse rastrear(@PathVariable String token) {
        return PedidoResponse.from(pedidoUseCase.buscarPorToken(token));
    }

    // ── Exception handlers ─────────────────────────────────────────────────────

    @ExceptionHandler(PedidoNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(PedidoNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(TransicaoInvalidaException.class)
    public ResponseEntity<Map<String, String>> handleTransicao(TransicaoInvalidaException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("error", e.getMessage()));
    }
}
