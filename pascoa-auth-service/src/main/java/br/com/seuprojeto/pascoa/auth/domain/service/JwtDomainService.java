package br.com.seuprojeto.pascoa.auth.domain.service;

import br.com.seuprojeto.pascoa.auth.domain.exception.TokenException;
import br.com.seuprojeto.pascoa.auth.domain.model.Token;
import br.com.seuprojeto.pascoa.auth.domain.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class JwtDomainService {

    private final SecretKey secretKey;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public JwtDomainService(String secret, Duration accessTtl, Duration refreshTtl) {
        this.secretKey  = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtl  = accessTtl;
        this.refreshTtl = refreshTtl;
    }

    public Token generate(Usuario usuario) {
        Instant now             = Instant.now();
        Instant accessExpires   = now.plus(accessTtl);
        Instant refreshExpires  = now.plus(refreshTtl);

        List<String> roles = usuario.getRoles().stream()
                .map(r -> r.authority())
                .collect(Collectors.toList());

        String access = Jwts.builder()
                .subject(String.valueOf(usuario.getId()))
                .claim("login", usuario.getLogin())
                .claim("roles", roles)
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(accessExpires))
                .signWith(secretKey)
                .compact();

        String refresh = Jwts.builder()
                .subject(String.valueOf(usuario.getId()))
                .claim("login", usuario.getLogin())
                .claim("type", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(refreshExpires))
                .signWith(secretKey)
                .compact();

        return Token.of(access, refresh, accessExpires, refreshExpires);
    }

    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw TokenException.expired();
        } catch (JwtException e) {
            throw TokenException.invalid();
        }
    }

    public Duration accessTtl() {
        return accessTtl;
    }
}
