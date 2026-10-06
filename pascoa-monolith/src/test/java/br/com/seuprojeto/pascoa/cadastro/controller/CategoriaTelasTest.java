package br.com.seuprojeto.pascoa.cadastro.controller;

import br.com.seuprojeto.pascoa.cadastro.entity.CategoriaProduto;
import br.com.seuprojeto.pascoa.cadastro.entity.Produto;
import br.com.seuprojeto.pascoa.cadastro.repository.CategoriaProdutoRepository;
import br.com.seuprojeto.pascoa.cadastro.service.ProdutoService;
import br.com.seuprojeto.pascoa.common.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CategoriaTelasTest {

    @Autowired private MockMvc mvc;
    @Autowired private CategoriaProdutoRepository categorias;
    @Autowired private ProdutoService produtoService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void listaFormECatalogo_mostramACategoriaDoProduto() throws Exception {
        String nomeCategoria = "Salgados-" + UUID.randomUUID();
        String nomeProduto = "Coxinha-" + UUID.randomUUID();
        TenantContext.executar(1L, () -> {
            CategoriaProduto categoria = categorias.save(CategoriaProduto.builder().nome(nomeCategoria).build());
            produtoService.salvar(Produto.builder().nome(nomeProduto).precoVenda(new BigDecimal("8.00"))
                .categoria(categoria).build());
            produtoService.salvar(Produto.builder().nome(nomeProduto + "-sem").precoVenda(BigDecimal.TEN).build());
        });

        mvc.perform(get("/produtos")).andExpect(status().isOk()).andExpect(content().string(containsString(nomeCategoria)));
        mvc.perform(get("/produtos/novo")).andExpect(status().isOk()).andExpect(content().string(containsString(nomeCategoria)));
        mvc.perform(get("/categorias")).andExpect(status().isOk()).andExpect(content().string(containsString(nomeCategoria)));
        mvc.perform(get("/categorias/novo")).andExpect(status().isOk());
        mvc.perform(get("/catalogo")).andExpect(status().isOk()).andExpect(content().string(containsString(nomeProduto)));
    }
}
