package br.com.seuprojeto.pascoa.customer.adapter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ClienteControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("pascoa_customers")
            .withUsername("postgres")
            .withPassword("sua_senha");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.autoconfigure.exclude",
                () -> "org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration");
    }

    @Autowired MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/clientes sem token retorna 401")
    void semTokenRetorna401() throws Exception {
        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/clientes com body inválido retorna 400")
    void bodyInvalidoRetorna400() throws Exception {
        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer token-invalido")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
