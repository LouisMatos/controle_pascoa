package br.com.seuprojeto.pascoa.auth.adapter.in.rest;

import br.com.seuprojeto.pascoa.auth.adapter.in.rest.dto.LoginRequest;
import br.com.seuprojeto.pascoa.auth.adapter.in.rest.dto.RefreshRequest;
import br.com.seuprojeto.pascoa.auth.adapter.in.rest.dto.TokenResponse;
import br.com.seuprojeto.pascoa.auth.application.port.in.AuthUseCase;
import br.com.seuprojeto.pascoa.auth.domain.exception.AccountBlockedException;
import br.com.seuprojeto.pascoa.auth.domain.exception.InvalidCredentialsException;
import br.com.seuprojeto.pascoa.auth.domain.exception.TokenException;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthUseCase authUseCase;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        var token = authUseCase.login(new AuthUseCase.LoginCommand(
                request.login(), request.senha(), request.totpCodigo()));
        return ResponseEntity.ok(TokenResponse.from(token));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        var token = authUseCase.refresh(request.refreshToken());
        return ResponseEntity.ok(TokenResponse.from(token));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String token = extractBearer(authHeader);
        authUseCase.logout(token);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate(
            @RequestHeader("Authorization") String authHeader) {
        String token = extractBearer(authHeader);
        Claims claims = authUseCase.validate(token);
        return ResponseEntity.ok(Map.of(
                "userId", claims.getSubject(),
                "login", claims.get("login"),
                "roles", claims.get("roles")
        ));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleInvalidCredentials(InvalidCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(AccountBlockedException.class)
    public ResponseEntity<Map<String, String>> handleBlocked(AccountBlockedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(TokenException.class)
    public ResponseEntity<Map<String, String>> handleToken(TokenException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", e.getMessage()));
    }

    private String extractBearer(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw TokenException.invalid();
    }
}
