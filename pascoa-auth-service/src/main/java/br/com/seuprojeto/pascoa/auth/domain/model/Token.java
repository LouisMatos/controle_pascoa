package br.com.seuprojeto.pascoa.auth.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class Token {

    private final String accessToken;
    private final String refreshToken;
    private final Instant accessExpiresAt;
    private final Instant refreshExpiresAt;
    private final String tokenType;

    public static Token of(String accessToken, String refreshToken,
                           Instant accessExpiresAt, Instant refreshExpiresAt) {
        return Token.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessExpiresAt(accessExpiresAt)
                .refreshExpiresAt(refreshExpiresAt)
                .tokenType("Bearer")
                .build();
    }
}
