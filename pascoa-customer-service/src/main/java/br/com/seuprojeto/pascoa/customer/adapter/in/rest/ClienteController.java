package br.com.seuprojeto.pascoa.customer.adapter.in.rest;

import br.com.seuprojeto.pascoa.customer.adapter.in.rest.dto.ClienteRequest;
import br.com.seuprojeto.pascoa.customer.adapter.in.rest.dto.ClienteResponse;
import br.com.seuprojeto.pascoa.customer.application.port.in.ClienteUseCase;
import br.com.seuprojeto.pascoa.customer.domain.exception.ClienteNotFoundException;
import br.com.seuprojeto.pascoa.customer.domain.exception.EmailDuplicadoException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteUseCase clienteUseCase;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE','ANALISTA','FINANCEIRO')")
    public List<ClienteResponse> listar() {
        return clienteUseCase.listar().stream()
                .map(ClienteResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE','ANALISTA','FINANCEIRO')")
    public ClienteResponse buscar(@PathVariable Long id) {
        return ClienteResponse.from(clienteUseCase.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
    public ResponseEntity<ClienteResponse> criar(@Valid @RequestBody ClienteRequest request) {
        var cliente = clienteUseCase.criar(new ClienteUseCase.CriarClienteCommand(
                request.nome(), request.email(), request.telefone(),
                request.cpf(), request.dataNascimento(),
                request.enderecoEntrega(), request.preferenciaCanal()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(ClienteResponse.from(cliente));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
    public ClienteResponse atualizar(@PathVariable Long id,
                                     @Valid @RequestBody ClienteRequest request) {
        var cliente = clienteUseCase.atualizar(new ClienteUseCase.AtualizarClienteCommand(
                id, request.nome(), request.email(), request.telefone(),
                request.enderecoEntrega(), request.preferenciaCanal()
        ));
        return ClienteResponse.from(cliente);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        clienteUseCase.inativar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/pontos")
    @PreAuthorize("hasAnyRole('ADMIN','ATENDENTE')")
    public ClienteResponse adicionarPontos(@PathVariable Long id,
                                           @RequestParam @Min(1) int pontos) {
        return ClienteResponse.from(clienteUseCase.adicionarPontos(id, pontos));
    }

    @ExceptionHandler(ClienteNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ClienteNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(EmailDuplicadoException.class)
    public ResponseEntity<Map<String, String>> handleDuplicate(EmailDuplicadoException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
    }
}
