package br.com.seuprojeto.pascoa.orcamento.controller;

import br.com.seuprojeto.pascoa.cadastro.entity.Cliente;
import br.com.seuprojeto.pascoa.cadastro.entity.PreferenciaCanal;
import br.com.seuprojeto.pascoa.cadastro.entity.Produto;
import br.com.seuprojeto.pascoa.cadastro.entity.UnidadeVenda;
import br.com.seuprojeto.pascoa.cadastro.repository.ClienteRepository;
import br.com.seuprojeto.pascoa.cadastro.repository.ProdutoRepository;
import br.com.seuprojeto.pascoa.common.tenant.TenantContext;
import br.com.seuprojeto.pascoa.orcamento.dto.OrcamentoForm;
import br.com.seuprojeto.pascoa.orcamento.dto.OrcamentoItemForm;
import br.com.seuprojeto.pascoa.orcamento.entity.Orcamento;
import br.com.seuprojeto.pascoa.orcamento.service.OrcamentoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "ADMIN")
class OrcamentoFormErrosTest {

    @Autowired private MockMvc mvc;
    @Autowired private ClienteRepository clientes;
    @Autowired private ProdutoRepository produtos;
    @Autowired private OrcamentoService service;

    private Cliente cliente() {
        return clientes.save(Cliente.builder().nome("C-" + UUID.randomUUID()).optIn(false)
            .preferenciaCanal(PreferenciaCanal.NENHUM).build());
    }

    private Produto cento() {
        return produtos.save(Produto.builder().nome("Coxinha-" + UUID.randomUUID())
            .precoVenda(new BigDecimal("80.00")).unidadeVenda(UnidadeVenda.CENTO).build());
    }

    @Test
    void novo_quantidadeFracionadaDeCento_reexibeFormComMensagem() throws Exception {
        Cliente c = cliente();
        Produto p = cento();

        mvc.perform(post("/orcamentos/novo").with(csrf())
                .param("clienteId", c.getId().toString())
                .param("validade", LocalDate.now().plusDays(5).toString())
                .param("itens[0].produtoId", p.getId().toString())
                .param("itens[0].quantidade", "1.5")
                .param("itens[0].precoUnitario", "80.00"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Cento só aceita quantidade inteira.")))
            .andExpect(content().string(containsString("role=\"alert\"")))
            .andExpect(content().string(containsString(p.getNome())));
    }

    @Test
    void editar_quantidadeAcimaDoMaximo_reexibeFormEMantemOrcamento() throws Exception {
        Cliente c = cliente();
        Produto p = cento();
        OrcamentoItemForm item = new OrcamentoItemForm();
        item.setProdutoId(p.getId());
        item.setQuantidade(new BigDecimal("2"));
        item.setPrecoUnitario(new BigDecimal("80.00"));
        OrcamentoForm form = new OrcamentoForm();
        form.setClienteId(c.getId());
        form.setValidade(LocalDate.now().plusDays(5));
        form.setItens(List.of(item));
        Long id = TenantContext.calcular(1L, () -> service.criar(form, "op").getId());

        mvc.perform(post("/orcamentos/{id}/editar", id).with(csrf())
                .param("clienteId", c.getId().toString())
                .param("validade", LocalDate.now().plusDays(5).toString())
                .param("itens[0].produtoId", p.getId().toString())
                .param("itens[0].quantidade", "10000000")
                .param("itens[0].precoUnitario", "80.00"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Quantidade acima do máximo permitido.")));

        Orcamento orc = TenantContext.calcular(1L, () -> {
            Orcamento o = service.buscarPorId(id);
            o.getItens().size();
            return o;
        });
        assertThat(orc.getItens()).hasSize(1);
        assertThat(orc.getTotal()).isEqualByComparingTo("160.00");
    }
}
