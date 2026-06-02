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

    // S-01 — CSP agora é setado dinamicamente pelo CspNonceFilter (com nonce por
    // request). Mantemos a constante removida; .contentSecurityPolicy(...) abaixo
    // foi também removido para evitar duplicação de header.

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
                // S-01 — CSP definido em CspNonceFilter (nonce por request)
            )
            .authorizeHttpRequests(auth -> auth
                // V9 Etapa 1.1 — Recursos estáticos NUNCA bloqueados (causa #3 de tela em branco)
                .requestMatchers("/css/**", "/js/**", "/images/**",
                                 "/fonts/**", "/webjars/**", "/favicon.ico",
                                 "/error/**").permitAll()
                // Acesso público
                .requestMatchers("/login", "/logout").permitAll()
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/2fa/**").permitAll()
                .requestMatchers("/manutencao").permitAll()
                .requestMatchers("/acompanhamento/**").permitAll()
                .requestMatchers("/orcamento-publico/**").permitAll()
                .requestMatchers("/catalogo/**").permitAll()
                .requestMatchers("/onboarding/**").permitAll()  // FASE 8 v7 — signup público
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
