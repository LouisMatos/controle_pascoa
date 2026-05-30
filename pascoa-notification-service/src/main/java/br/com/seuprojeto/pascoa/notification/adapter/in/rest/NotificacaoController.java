package br.com.seuprojeto.pascoa.notification.adapter.in.rest;

import br.com.seuprojeto.pascoa.notification.application.port.in.NotificacaoUseCase;
import br.com.seuprojeto.pascoa.notification.domain.exception.NotificacaoNotFoundException;
import br.com.seuprojeto.pascoa.notification.domain.model.Notificacao;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notificacoes")
@RequiredArgsConstructor
public class NotificacaoController {

    private final NotificacaoUseCase notificacaoUseCase;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public List<Notificacao> listarRecentes(
            @RequestParam(defaultValue = "50") int limite) {
        return notificacaoUseCase.listarRecentes(limite);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Notificacao buscar(@PathVariable Long id) {
        return notificacaoUseCase.buscarPorId(id);
    }

    @GetMapping("/referencia/{referenciaId}")
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
    public List<Notificacao> porReferencia(@PathVariable String referenciaId) {
        return notificacaoUseCase.listarPorReferencia(referenciaId);
    }

    @PostMapping("/{id}/reenviar")
    @PreAuthorize("hasRole('ADMIN')")
    public Notificacao reenviar(@PathVariable Long id) {
        return notificacaoUseCase.reenviar(id);
    }

    @ExceptionHandler(NotificacaoNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NotificacaoNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }
}
