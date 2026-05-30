package br.com.seuprojeto.pascoa.config;

import br.com.seuprojeto.pascoa.seguranca.service.TwoFactorAuthenticationSuccessHandler;
import br.com.seuprojeto.pascoa.seguranca.service.UsuarioService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authProvider(@Lazy UsuarioService usuarioService) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(usuarioService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Repositório explícito para que o TwoFactorController possa injetar e chamar
     * saveContext() ao completar o fluxo de 2FA. Sem esse bean explícito, o Spring
     * Security 6 só registra o repositório como "shared object" do HttpSecurity,
     * que não é injetável em controllers.
     *
     * Combina:
     *  - RequestAttributeSecurityContextRepository: cache curto do request atual
     *    (necessário para SecurityContextHolderFilter ver o context após saveContext()
     *    em forwards/erros dentro do mesmo request).
     *  - HttpSessionSecurityContextRepository: persistência entre requests via JSESSIONID.
     */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new DelegatingSecurityContextRepository(
            new RequestAttributeSecurityContextRepository(),
            new HttpSessionSecurityContextRepository()
        );
    }

    private static final String CSP =
        "default-src 'self'; " +
        "script-src 'self' cdn.jsdelivr.net; " +
        "style-src 'self' 'unsafe-inline' cdn.jsdelivr.net; " +
        "font-src 'self' cdn.jsdelivr.net; " +
        "img-src 'self' data:; " +
        "connect-src 'self'; " +
        "frame-ancestors 'none'";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                    TwoFactorAuthenticationSuccessHandler twoFactorHandler,
                                                    SecurityContextRepository securityContextRepository) throws Exception {
        http
            .securityContext(sc -> sc.securityContextRepository(securityContextRepository))
            .headers(h -> h
                .frameOptions(f -> f.deny())
                .contentTypeOptions(Customizer.withDefaults())
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31_536_000))
                .referrerPolicy(rp -> rp
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives(CSP))
            )
            .authorizeHttpRequests(auth -> auth
                // Acesso público
                .requestMatchers("/login", "/logout").permitAll()
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/2fa/**").permitAll()
                .requestMatchers("/manutencao").permitAll()
                .requestMatchers("/acompanhamento/**").permitAll()
                .requestMatchers("/orcamento-publico/**").permitAll()
                .requestMatchers("/catalogo/**").permitAll()
                .requestMatchers("/uploads/**").permitAll()  // legado — mantido por compatibilidade
                .requestMatchers("/media/**").permitAll()
                // PWA
                .requestMatchers("/manifest.json", "/sw.js", "/icons/**").permitAll()
                // Apenas ADMIN
                .requestMatchers("/usuarios/**", "/notificacoes/**", "/auditoria/**", "/lgpd/**").hasRole("ADMIN")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // Actuator — somente ADMIN (health com detalhes, métricas)
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                // ADMIN e ATENDENTE — CRM
                .requestMatchers("/crm/**").hasAnyRole("ADMIN", "ATENDENTE")
                // ADMIN, FINANCEIRO e ANALISTA — relatórios e analytics
                .requestMatchers("/financeiro/**", "/analytics/**").hasAnyRole("ADMIN", "FINANCEIRO", "ANALISTA")
                // ADMIN e FINANCEIRO — gestão de gastos (lançamentos, importação)
                .requestMatchers("/gastos/**").hasAnyRole("ADMIN", "FINANCEIRO")
                // ADMIN, CONFEITEIRO e GESTOR_QUALIDADE — produção e qualidade
                .requestMatchers("/producao/**", "/qualidade/**").hasAnyRole("ADMIN", "CONFEITEIRO", "GESTOR_QUALIDADE")
                // Qualquer autenticado para o restante
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(twoFactorHandler)
                .failureUrl("/login?erro")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );
        return http.build();
    }
}
