package br.com.seuprojeto.pascoa.seguranca;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração de permissões por role.
 * Verifica que cada perfil tem acesso apenas aos recursos permitidos.
 *
 * <p>Para rotas "permitidas", o teste confirma que o status HTTP é diferente de 403
 * (pode ser 200, 302 redirect, 404, 500 — qualquer coisa menos bloqueio de autorização).
 * Para rotas "bloqueadas", confirma exatamente o 403 Forbidden.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RolePermissionsTest {

    @Autowired
    private MockMvc mockMvc;

    /** Asserta que a resposta NÃO é 403 (usuário está autorizado). */
    private static org.springframework.test.web.servlet.ResultMatcher naoForbidden() {
        return result -> assertThat(result.getResponse().getStatus())
                .as("Esperado acesso permitido (status != 403)")
                .isNotEqualTo(403);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GESTOR_QUALIDADE — produção e qualidade ✓ | financeiro/gastos ✗
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("GESTOR_QUALIDADE acessa /qualidade sem restrição")
    @WithMockUser(roles = "GESTOR_QUALIDADE")
    void gestorQualidade_podeAcessarQualidade() throws Exception {
        mockMvc.perform(get("/qualidade"))
               .andExpect(naoForbidden());
    }

    @Test
    @DisplayName("GESTOR_QUALIDADE acessa /producao sem restrição")
    @WithMockUser(roles = "GESTOR_QUALIDADE")
    void gestorQualidade_podeAcessarProducao() throws Exception {
        mockMvc.perform(get("/producao"))
               .andExpect(naoForbidden());
    }

    @Test
    @DisplayName("GESTOR_QUALIDADE recebe 403 ao acessar /financeiro/dashboard")
    @WithMockUser(roles = "GESTOR_QUALIDADE")
    void gestorQualidade_bloqueadoEmFinanceiro() throws Exception {
        mockMvc.perform(get("/financeiro/dashboard"))
               .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GESTOR_QUALIDADE recebe 403 ao acessar /gastos")
    @WithMockUser(roles = "GESTOR_QUALIDADE")
    void gestorQualidade_bloqueadoEmGastos() throws Exception {
        mockMvc.perform(get("/gastos"))
               .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GESTOR_QUALIDADE recebe 403 ao acessar /usuarios")
    @WithMockUser(roles = "GESTOR_QUALIDADE")
    void gestorQualidade_bloqueadoEmUsuarios() throws Exception {
        mockMvc.perform(get("/usuarios"))
               .andExpect(status().isForbidden());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ANALISTA — financeiro/analytics ✓ | gastos/crm/producao ✗
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("ANALISTA acessa /analytics sem restrição")
    @WithMockUser(roles = "ANALISTA")
    void analista_podeAcessarAnalytics() throws Exception {
        mockMvc.perform(get("/analytics"))
               .andExpect(naoForbidden());
    }

    @Test
    @DisplayName("ANALISTA acessa /financeiro/dashboard sem restrição")
    @WithMockUser(roles = "ANALISTA")
    void analista_podeAcessarFinanceiroDashboard() throws Exception {
        mockMvc.perform(get("/financeiro/dashboard"))
               .andExpect(naoForbidden());
    }

    @Test
    @DisplayName("ANALISTA recebe 403 ao acessar /gastos")
    @WithMockUser(roles = "ANALISTA")
    void analista_bloqueadoEmGastos() throws Exception {
        mockMvc.perform(get("/gastos"))
               .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ANALISTA recebe 403 ao acessar /crm")
    @WithMockUser(roles = "ANALISTA")
    void analista_bloqueadoEmCrm() throws Exception {
        mockMvc.perform(get("/crm"))
               .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ANALISTA recebe 403 ao acessar /producao")
    @WithMockUser(roles = "ANALISTA")
    void analista_bloqueadoEmProducao() throws Exception {
        mockMvc.perform(get("/producao"))
               .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ANALISTA recebe 403 ao acessar /usuarios")
    @WithMockUser(roles = "ANALISTA")
    void analista_bloqueadoEmUsuarios() throws Exception {
        mockMvc.perform(get("/usuarios"))
               .andExpect(status().isForbidden());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CONFEITEIRO — produção ✓ | financeiro/gastos/crm ✗
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("CONFEITEIRO acessa /producao/kanban sem restrição")
    @WithMockUser(roles = "CONFEITEIRO")
    void confeiteiro_podeAcessarProducaoKanban() throws Exception {
        mockMvc.perform(get("/producao/kanban"))
               .andExpect(naoForbidden());
    }

    @Test
    @DisplayName("CONFEITEIRO recebe 403 ao acessar /financeiro/dashboard")
    @WithMockUser(roles = "CONFEITEIRO")
    void confeiteiro_bloqueadoEmFinanceiro() throws Exception {
        mockMvc.perform(get("/financeiro/dashboard"))
               .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CONFEITEIRO recebe 403 ao acessar /gastos")
    @WithMockUser(roles = "CONFEITEIRO")
    void confeiteiro_bloqueadoEmGastos() throws Exception {
        mockMvc.perform(get("/gastos"))
               .andExpect(status().isForbidden());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FINANCEIRO — gastos/financeiro ✓ | producao/qualidade/usuarios ✗
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("FINANCEIRO acessa /gastos sem restrição")
    @WithMockUser(roles = "FINANCEIRO")
    void financeiro_podeAcessarGastos() throws Exception {
        mockMvc.perform(get("/gastos"))
               .andExpect(naoForbidden());
    }

    @Test
    @DisplayName("FINANCEIRO acessa /financeiro/dashboard sem restrição")
    @WithMockUser(roles = "FINANCEIRO")
    void financeiro_podeAcessarFinanceiroDashboard() throws Exception {
        mockMvc.perform(get("/financeiro/dashboard"))
               .andExpect(naoForbidden());
    }

    @Test
    @DisplayName("FINANCEIRO recebe 403 ao acessar /producao")
    @WithMockUser(roles = "FINANCEIRO")
    void financeiro_bloqueadoEmProducao() throws Exception {
        mockMvc.perform(get("/producao"))
               .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("FINANCEIRO recebe 403 ao acessar /usuarios")
    @WithMockUser(roles = "FINANCEIRO")
    void financeiro_bloqueadoEmUsuarios() throws Exception {
        mockMvc.perform(get("/usuarios"))
               .andExpect(status().isForbidden());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ADMIN — acesso irrestrito a todos os módulos
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("ADMIN acessa /usuarios sem restrição")
    @WithMockUser(roles = "ADMIN")
    void admin_podeAcessarUsuarios() throws Exception {
        mockMvc.perform(get("/usuarios"))
               .andExpect(naoForbidden());
    }

    @Test
    @DisplayName("ADMIN acessa /gastos sem restrição")
    @WithMockUser(roles = "ADMIN")
    void admin_podeAcessarGastos() throws Exception {
        mockMvc.perform(get("/gastos"))
               .andExpect(naoForbidden());
    }

    @Test
    @DisplayName("ADMIN acessa /producao sem restrição")
    @WithMockUser(roles = "ADMIN")
    void admin_podeAcessarProducao() throws Exception {
        mockMvc.perform(get("/producao"))
               .andExpect(naoForbidden());
    }

    @Test
    @DisplayName("ADMIN acessa /financeiro/dashboard sem restrição")
    @WithMockUser(roles = "ADMIN")
    void admin_podeAcessarFinanceiro() throws Exception {
        mockMvc.perform(get("/financeiro/dashboard"))
               .andExpect(naoForbidden());
    }
}
