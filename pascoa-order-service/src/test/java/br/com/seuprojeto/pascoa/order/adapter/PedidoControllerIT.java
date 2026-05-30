package br.com.seuprojeto.pascoa.order.adapter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PedidoControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("pascoa_orders")
            .withUsername("postgres")
            .withPassword("sua_senha");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.autoconfigure.exclude",
                () -> "org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration," +
                      "org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration," +
                      "org.springframework.cloud.netflix.eureka.EurekaDiscoveryClientAutoConfiguration");
    }

    @Autowired MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/pedidos sem token retorna 401")
    void semTokenRetorna401() throws Exception {
        mockMvc.perform(get("/api/pedidos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/acompanhamento/{token} público retorna 404 para token inexistente")
    void rastreamentoPublico404() throws Exception {
        mockMvc.perform(get("/api/acompanhamento/TOKENINVALIDO"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /actuator/health retorna UP")
    void actuatorHealth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
