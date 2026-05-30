package br.com.seuprojeto.pascoa.auth.application.usecase;

import br.com.seuprojeto.pascoa.auth.application.port.in.AuthUseCase;
import br.com.seuprojeto.pascoa.auth.application.port.out.AuthEventPublisherPort;
import br.com.seuprojeto.pascoa.auth.application.port.out.TokenBlacklistPort;
import br.com.seuprojeto.pascoa.auth.application.port.out.UserRepositoryPort;
import br.com.seuprojeto.pascoa.auth.domain.exception.AccountBlockedException;
import br.com.seuprojeto.pascoa.auth.domain.exception.InvalidCredentialsException;
import br.com.seuprojeto.pascoa.auth.domain.exception.TokenException;
import br.com.seuprojeto.pascoa.auth.domain.model.Token;
import br.com.seuprojeto.pascoa.auth.domain.model.Usuario;
import br.com.seuprojeto.pascoa.auth.domain.service.JwtDomainService;
import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.time.Instant;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthUseCaseImpl implements AuthUseCase {

    private final UserRepositoryPort userRepository;
    private final TokenBlacklistPort tokenBlacklist;
    private final AuthEventPublisherPort eventPublisher;
    private final JwtDomainService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final TimeBasedOneTimePasswordGenerator totp;

    @Override
    public Token login(LoginCommand command) {
        Usuario usuario = userRepository.findByLogin(command.login())
                .orElseThrow(() -> {
                    eventPublisher.publishLoginFailed(command.login(), "usuario_nao_encontrado");
                    return new InvalidCredentialsException();
                });

        if (!usuario.isAtivo()) {
            eventPublisher.publishLoginFailed(command.login(), "conta_inativa");
            throw new InvalidCredentialsException();
        }

        if (usuario.estaBloqueado()) {
            eventPublisher.publishLoginFailed(command.login(), "conta_bloqueada");
            throw new AccountBlockedException();
        }

        if (!passwordEncoder.matches(command.senha(), usuario.getPasswordHash())) {
            Usuario atualizado = userRepository.save(usuario.incrementarFalhas());
            eventPublisher.publishLoginFailed(command.login(), "senha_incorreta");
            if (atualizado.estaBloqueado()) throw new AccountBlockedException();
            throw new InvalidCredentialsException();
        }

        if (usuario.isTotpAtivado()) {
            if (command.totpCodigo() == null) {
                throw new InvalidCredentialsException();
            }
            if (!validarTotp(usuario.getTotpSecret(), command.totpCodigo())) {
                eventPublisher.publishLoginFailed(command.login(), "totp_invalido");
                throw new InvalidCredentialsException();
            }
        }

        userRepository.save(usuario.resetarFalhas());

        Token token = jwtService.generate(usuario);
        eventPublisher.publishLoginSuccess(usuario.getId(), usuario.getLogin());
        log.info("Login bem-sucedido: {}", usuario.getLogin());
        return token;
    }

    @Override
    @Transactional(readOnly = true)
    public Token refresh(String refreshToken) {
        if (tokenBlacklist.isBlacklisted(refreshToken)) throw TokenException.blacklisted();

        Claims claims = jwtService.parse(refreshToken);
        if (!"refresh".equals(claims.get("type"))) throw TokenException.invalid();

        Long userId = Long.parseLong(claims.getSubject());
        Usuario usuario = userRepository.findById(userId)
                .orElseThrow(TokenException::invalid);

        return jwtService.generate(usuario);
    }

    @Override
    public void logout(String accessToken) {
        if (tokenBlacklist.isBlacklisted(accessToken)) return;

        Claims claims = jwtService.parse(accessToken);
        Instant expiry = claims.getExpiration().toInstant();
        java.time.Duration ttl = java.time.Duration.between(Instant.now(), expiry);

        if (!ttl.isNegative()) {
            tokenBlacklist.add(accessToken, ttl);
            log.info("Logout: token revogado para usuário {}", claims.get("login"));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Claims validate(String accessToken) {
        if (tokenBlacklist.isBlacklisted(accessToken)) throw TokenException.blacklisted();
        Claims claims = jwtService.parse(accessToken);
        if (!"access".equals(claims.get("type"))) throw TokenException.invalid();
        return claims;
    }

    private boolean validarTotp(String secret, int codigo) {
        try {
            byte[] decoded = Base64.getDecoder().decode(secret);
            SecretKey key = new SecretKeySpec(decoded, "HmacSHA1");
            int expected = totp.generateOneTimePassword(key, Instant.now());
            return expected == codigo;
        } catch (InvalidKeyException e) {
            log.error("Erro ao validar TOTP", e);
            return false;
        }
    }
}
