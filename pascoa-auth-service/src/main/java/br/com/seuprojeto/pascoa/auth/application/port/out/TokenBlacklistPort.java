package br.com.seuprojeto.pascoa.auth.application.port.out;

import java.time.Duration;

public interface TokenBlacklistPort {
    void add(String token, Duration ttl);
    boolean isBlacklisted(String token);
}
