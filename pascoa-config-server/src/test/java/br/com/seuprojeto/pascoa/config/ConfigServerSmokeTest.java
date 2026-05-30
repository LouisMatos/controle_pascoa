package br.com.seuprojeto.pascoa.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ConfigServerSmokeTest {

    @Autowired MockMvc mockMvc;

    @Test
    @DisplayName("Config Server sobe e serve configs do pascoa-auth-service")
    void serveConfigAuthService() throws Exception {
        mockMvc.perform(get("/pascoa-auth-service/default")
                        .with(httpBasic("config-admin", "config123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("pascoa-auth-service"));
    }

    @Test
    @DisplayName("Acesso sem autenticação retorna 401")
    void semAutenticacaoRetorna401() throws Exception {
        mockMvc.perform(get("/pascoa-auth-service/default"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Actuator health é público")
    void actuatorHealthPublico() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
