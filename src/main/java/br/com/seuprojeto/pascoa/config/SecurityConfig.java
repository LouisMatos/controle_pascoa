package br.com.seuprojeto.pascoa.config;

import br.com.seuprojeto.pascoa.seguranca.service.UsuarioService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
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

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Acesso público
                .requestMatchers("/login", "/logout").permitAll()
                .requestMatchers("/acompanhamento/**").permitAll()
                .requestMatchers("/orcamento-publico/**").permitAll()
                .requestMatchers("/catalogo/**").permitAll()
                .requestMatchers("/uploads/**").permitAll()
                // PWA
                .requestMatchers("/manifest.json", "/sw.js", "/icons/**").permitAll()
                // Apenas ADMIN
                .requestMatchers("/usuarios/**", "/notificacoes/**").hasRole("ADMIN")
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
                .defaultSuccessUrl("/dashboard", true)
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
