package br.com.seuprojeto.pascoa.common.tenant;

import br.com.seuprojeto.pascoa.cadastro.entity.Cliente;
import br.com.seuprojeto.pascoa.cadastro.entity.Fornecedor;
import br.com.seuprojeto.pascoa.cadastro.entity.PreferenciaCanal;
import br.com.seuprojeto.pascoa.cadastro.repository.ClienteRepository;
import br.com.seuprojeto.pascoa.cadastro.repository.FornecedorRepository;
import br.com.seuprojeto.pascoa.seguranca.entity.Role;
import br.com.seuprojeto.pascoa.seguranca.entity.Usuario;
import br.com.seuprojeto.pascoa.seguranca.service.UsuarioPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TenantCadeiaMvcTest {

    @Autowired private MockMvc mvc;
    @Autowired private ClienteRepository clientes;
    @Autowired private FornecedorRepository fornecedores;

    private RequestPostProcessor usuarioDaLoja2() {
        var usuario = Usuario.builder().nome("L2").login("l2-" + UUID.randomUUID()).senha("x")
            .role(Role.ADMIN).lojaId(2L).build();
        return user(new UsuarioPrincipal(usuario));
    }

    @Test
    void usuarioDaLoja2_naoEnxergaClienteDaLoja1() throws Exception {
        Long id = TenantContext.calcular(1L, () -> clientes.save(Cliente.builder()
            .nome("Cliente-" + UUID.randomUUID()).optIn(false).preferenciaCanal(PreferenciaCanal.NENHUM).build()).getId());

        mvc.perform(get("/clientes/{id}/editar", id).with(usuarioDaLoja2()))
            .andExpect(status().is3xxRedirection())
            .andExpect(flash().attributeExists("erro"));
    }

    @Test
    void usuarioDaLoja2_naoSobrescreveFornecedorDaLoja1() throws Exception {
        String nome = "Fornecedor-" + UUID.randomUUID();
        Long id = TenantContext.calcular(1L, () -> fornecedores.save(Fornecedor.builder().nome(nome).build()).getId());

        assertThatThrownBy(() -> mvc.perform(post("/fornecedores/salvar").with(usuarioDaLoja2()).with(csrf())
            .param("id", id.toString()).param("nome", "hack")))
            .hasStackTraceContaining("EntityNotFoundException");

        assertThat(TenantContext.calcular(1L, () -> fornecedores.findById(id).orElseThrow().getNome()))
            .isEqualTo(nome);
    }
}
