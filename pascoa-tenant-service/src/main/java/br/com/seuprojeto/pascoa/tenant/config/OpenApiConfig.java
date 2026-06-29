package br.com.seuprojeto.pascoa.tenant.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * v6 Etapa 15 — Documentação da API pública FoodFlow.
 *
 * <p>Acessível em:
 * <ul>
 *   <li>{@code /v3/api-docs} — JSON OpenAPI 3.0</li>
 *   <li>{@code /swagger-ui.html} — UI interativa</li>
 * </ul>
 *
 * <p>API pública requer:
 * <ol>
 *   <li>Plano ENTERPRISE (feature flag {@code API_PUBLICA} ativada)</li>
 *   <li>Cabeçalho {@code X-API-Key} gerado em {@code POST /tenants/{id}/api-keys}</li>
 * </ol>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI foodflowOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FoodFlow Tenant Service — API pública")
                        .description("""
                                API REST do FoodFlow v6 — gestão de tenants, white-label, feature flags
                                e onboarding self-service.

                                **Disponibilidade da API pública:** apenas tenants com plano ENTERPRISE
                                têm a feature flag `API_PUBLICA` ativada por default. Tenants em outros planos
                                podem solicitar liberação manual ao admin da plataforma.

                                **Autenticação:** cabeçalho `X-API-Key` (gerado em
                                `POST /tenants/{id}/api-keys`). A chave plain só é exibida UMA vez na criação;
                                o servidor armazena apenas o hash SHA-256.
                                """)
                        .version("v6.0")
                        .contact(new Contact()
                                .name("FoodFlow Platform")
                                .email("api@foodflow.com.br"))
                        .license(new License().name("Proprietária")))
                .addSecurityItem(new SecurityRequirement().addList("ApiKey"))
                .components(new Components()
                        .addSecuritySchemes("ApiKey", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-API-Key")
                                .description("Chave de API do tenant (exibida apenas na criação)")));
    }
}
