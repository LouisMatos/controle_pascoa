package br.com.seuprojeto.pascoa.auth.config;

import br.com.seuprojeto.pascoa.auth.domain.service.JwtDomainService;
import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class JwtConfig {

    @Value("${auth.jwt.secret}")
    private String secret;

    @Value("${auth.jwt.access-ttl:15m}")
    private Duration accessTtl;

    @Value("${auth.jwt.refresh-ttl:7d}")
    private Duration refreshTtl;

    @Bean
    public JwtDomainService jwtDomainService() {
        return new JwtDomainService(secret, accessTtl, refreshTtl);
    }

    @Bean
    public TimeBasedOneTimePasswordGenerator totpGenerator() {
        return new TimeBasedOneTimePasswordGenerator();
    }
}
