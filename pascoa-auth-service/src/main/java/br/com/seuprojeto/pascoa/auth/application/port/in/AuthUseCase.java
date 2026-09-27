package br.com.seuprojeto.pascoa.auth.application.port.in;

import br.com.seuprojeto.pascoa.auth.domain.model.Token;
import io.jsonwebtoken.Claims;

public interface AuthUseCase {

    record LoginCommand(String login, String senha, Integer totpCodigo) {}

    Token login(LoginCommand command);

    Token refresh(String refreshToken);

    void logout(String accessToken);

    Claims validate(String accessToken);
}
