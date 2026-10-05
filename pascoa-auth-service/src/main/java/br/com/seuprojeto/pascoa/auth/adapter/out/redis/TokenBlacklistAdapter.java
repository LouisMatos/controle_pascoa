package br.com.seuprojeto.pascoa.auth.adapter.out.redis;

import br.com.seuprojeto.pascoa.auth.application.port.out.TokenBlacklistPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class TokenBlacklistAdapter implements TokenBlacklistPort {

    private static final String PREFIX = "auth:blacklist:";

    private final StringRedisTemplate redis;

    @Override
    public void add(String token, Duration ttl) {
        redis.opsForValue().set(PREFIX + token, "1", ttl);
    }

    @Override
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redis.hasKey(PREFIX + token));
    }
}
