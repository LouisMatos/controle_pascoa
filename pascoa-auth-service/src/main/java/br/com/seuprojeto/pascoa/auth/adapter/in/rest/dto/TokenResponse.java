package br.com.seuprojeto.pascoa.auth.adapter.in.rest.dto;

import br.com.seuprojeto.pascoa.auth.domain.model.Token;

import java.time.Instant;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Instant accessExpiresAt,
        Instant refreshExpiresAt
) {
    public static TokenResponse from(Token token) {
        return new TokenResponse(
                token.getAccessToken(),
                token.getRefreshToken(),
                token.getTokenType(),
                token.getAccessExpiresAt(),
                token.getRefreshExpiresAt()
        );
    }
}
