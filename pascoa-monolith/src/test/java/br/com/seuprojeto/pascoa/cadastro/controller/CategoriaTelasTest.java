package br.com.seuprojeto.pascoa.cadastro.controller;

import br.com.seuprojeto.pascoa.cadastro.entity.CategoriaProduto;
import br.com.seuprojeto.pascoa.cadastro.entity.Produto;
import br.com.seuprojeto.pascoa.cadastro.repository.CategoriaProdutoRepository;
import br.com.seuprojeto.pascoa.cadastro.service.ProdutoService;
import br.com.seuprojeto.pascoa.cadastro.entity.UnidadeVenda;
import br.com.seuprojeto.pascoa.common.tenant.TenantContext;
import br.com.seuprojeto.pascoa.fichaTecnica.service.FichaTecnicaService;
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
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CategoriaTelasTest {

    @Autowired private MockMvc mvc;
    @Autowired private CategoriaProdutoRepository categorias;
    @Autowired private ProdutoService produtoService;
    @Autowired private FichaTecnicaService fichaService;

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

    @Test
    @WithMockUser(roles = "ADMIN")
    void editarProdutoComCategoriaInativa_mantemACategoriaSelecionada() throws Exception {
        String nomeCategoria = "Inativa-" + UUID.randomUUID();
        Long id = TenantContext.calcular(1L, () -> {
            CategoriaProduto categoria = categorias.save(CategoriaProduto.builder().nome(nomeCategoria).ativo(false).build());
            return produtoService.salvar(Produto.builder().nome("Prod-" + UUID.randomUUID())
                .precoVenda(BigDecimal.TEN).categoria(categoria).build()).getId();
        });

        mvc.perform(get("/produtos/{id}/editar", id)).andExpect(status().isOk())
            .andExpect(content().string(matchesPattern("(?s).*<option[^>]*selected[^>]*>" + nomeCategoria + " \\(inativa\\)</option>.*")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void catalogo_categoriaInvalidaMostraTudo_eCategoriaValidaFiltra() throws Exception {
        String nomeCategoria = "Filtro-" + UUID.randomUUID();
        String dentro = "Dentro-" + UUID.randomUUID();
        String fora = "Fora-" + UUID.randomUUID();
        Long categoriaId = TenantContext.calcular(1L, () -> {
            CategoriaProduto categoria = categorias.save(CategoriaProduto.builder().nome(nomeCategoria).build());
            produtoService.salvar(Produto.builder().nome(dentro).precoVenda(BigDecimal.TEN).categoria(categoria).build());
            produtoService.salvar(Produto.builder().nome(fora).precoVenda(BigDecimal.TEN).build());
            return categoria.getId();
        });

        mvc.perform(get("/catalogo").param("categoria", "TRUFADO")).andExpect(status().isOk())
            .andExpect(content().string(containsString(dentro))).andExpect(content().string(containsString(fora)));
        mvc.perform(get("/catalogo").param("categoria", categoriaId.toString())).andExpect(status().isOk())
            .andExpect(content().string(containsString(dentro))).andExpect(content().string(not(containsString(fora))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void fichaTecnicaEFormDeProduto_mostramAUnidadeDeVenda() throws Exception {
        Long id = TenantContext.calcular(1L, () -> {
            Long produtoId = produtoService.salvar(Produto.builder().nome("Brigadeiro-" + UUID.randomUUID())
                .precoVenda(new BigDecimal("90.00")).unidadeVenda(UnidadeVenda.CENTO).build()).getId();
            fichaService.salvarInfo(produtoId, new BigDecimal("2"), null);
            return produtoId;
        });

        mvc.perform(get("/fichas/{id}", id)).andExpect(status().isOk()).andExpect(content().string(containsString("Cento")));
        mvc.perform(get("/produtos/novo")).andExpect(status().isOk()).andExpect(content().string(containsString("Vendido por")));
    }
}
