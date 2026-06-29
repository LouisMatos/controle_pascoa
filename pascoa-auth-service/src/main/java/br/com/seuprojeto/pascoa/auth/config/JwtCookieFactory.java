package br.com.seuprojeto.pascoa.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * S-02 — fábrica de cookies httpOnly para os JWTs.
 *
 * Cookies httpOnly não são acessíveis via JavaScript, eliminando o vetor de
 * roubo de token por XSS que existia quando o front-end armazenava o JWT em
 * localStorage. SameSite=Strict mitiga CSRF.
 */
@Component
public class JwtCookieFactory {

    public static final String ACCESS_COOKIE  = "access_token";
    public static final String REFRESH_COOKIE = "refresh_token";

    private final boolean secure;
    private final String  sameSite;
    private final String  domain;

    public JwtCookieFactory(@Value("${auth.cookie.secure:true}") boolean secure,
                            @Value("${auth.cookie.same-site:Strict}") String sameSite,
                            @Value("${auth.cookie.domain:}") String domain) {
        this.secure   = secure;
        this.sameSite = sameSite;
        this.domain   = (domain == null || domain.isBlank()) ? null : domain;
    }

    public ResponseCookie access(String token, Instant expiresAt) {
        return build(ACCESS_COOKIE, token, "/", ttl(expiresAt));
    }

    public ResponseCookie refresh(String token, Instant expiresAt) {
        // path "/api/auth" — refresh só é enviado nos endpoints de autenticação.
        return build(REFRESH_COOKIE, token, "/api/auth", ttl(expiresAt));
    }

    public ResponseCookie clearAccess()  { return build(ACCESS_COOKIE,  "", "/",          Duration.ZERO); }
    public ResponseCookie clearRefresh() { return build(REFRESH_COOKIE, "", "/api/auth",  Duration.ZERO); }

    private ResponseCookie build(String name, String value, String path, Duration maxAge) {
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(path)
                .maxAge(maxAge);
        if (domain != null) b.domain(domain);
        return b.build();
    }

    private Duration ttl(Instant expiresAt) {
        if (expiresAt == null) return Duration.ofHours(1);
        Duration d = Duration.between(Instant.now(), expiresAt);
        return d.isNegative() ? Duration.ZERO : d;
    }
}
