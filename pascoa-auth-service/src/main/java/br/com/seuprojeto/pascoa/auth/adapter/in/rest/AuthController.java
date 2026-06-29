package br.com.seuprojeto.pascoa.auth.adapter.in.rest;

import br.com.seuprojeto.pascoa.auth.adapter.in.rest.dto.LoginRequest;
import br.com.seuprojeto.pascoa.auth.adapter.in.rest.dto.RefreshRequest;
import br.com.seuprojeto.pascoa.auth.adapter.in.rest.dto.TokenResponse;
import br.com.seuprojeto.pascoa.auth.application.port.in.AuthUseCase;
import br.com.seuprojeto.pascoa.auth.config.JwtCookieFactory;
import br.com.seuprojeto.pascoa.auth.domain.exception.AccountBlockedException;
import br.com.seuprojeto.pascoa.auth.domain.exception.InvalidCredentialsException;
import br.com.seuprojeto.pascoa.auth.domain.exception.TokenException;
import br.com.seuprojeto.pascoa.auth.domain.model.Token;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthUseCase authUseCase;
    private final JwtCookieFactory cookies;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        Token token = authUseCase.login(new AuthUseCase.LoginCommand(
                request.login(), request.senha(), request.totpCodigo()));
        return withAuthCookies(token);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody(required = false) RefreshRequest request,
                                                 HttpServletRequest httpReq) {
        // S-02 — refresh token vem do cookie httpOnly por padrão; cai pro body
        // como fallback para manter compatibilidade com clientes antigos.
        String refreshToken = extractCookie(httpReq, JwtCookieFactory.REFRESH_COOKIE);
        if (refreshToken == null && request != null) refreshToken = request.refreshToken();
        if (refreshToken == null) throw TokenException.invalid();
        Token token = authUseCase.refresh(refreshToken);
        return withAuthCookies(token);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                       HttpServletRequest httpReq) {
        // S-02 — aceita token via cookie httpOnly OU header Authorization.
        String token = extractCookie(httpReq, JwtCookieFactory.ACCESS_COOKIE);
        if (token == null) token = extractBearer(authHeader);
        authUseCase.logout(token);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookies.clearAccess().toString())
                .header(HttpHeaders.SET_COOKIE, cookies.clearRefresh().toString())
                .build();
    }

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest httpReq) {
        String token = extractCookie(httpReq, JwtCookieFactory.ACCESS_COOKIE);
        if (token == null) token = extractBearer(authHeader);
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

    // ── helpers ──────────────────────────────────────────────────────────

    private ResponseEntity<TokenResponse> withAuthCookies(Token token) {
        // S-02 — emite os tokens em cookies httpOnly E mantém JSON na resposta
        // por compatibilidade com clientes server-to-server.
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        cookies.access(token.getAccessToken(), token.getAccessExpiresAt()).toString())
                .header(HttpHeaders.SET_COOKIE,
                        cookies.refresh(token.getRefreshToken(), token.getRefreshExpiresAt()).toString())
                .body(TokenResponse.from(token));
    }

    private String extractBearer(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw TokenException.invalid();
    }

    private String extractCookie(HttpServletRequest req, String name) {
        Cookie[] all = req.getCookies();
        if (all == null) return null;
        for (Cookie c : all) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }
}
