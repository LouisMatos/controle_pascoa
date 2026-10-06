package br.com.seuprojeto.pascoa.cadastro.controller;

import br.com.seuprojeto.pascoa.cadastro.entity.Cliente;
import br.com.seuprojeto.pascoa.cadastro.entity.Produto;
import br.com.seuprojeto.pascoa.cadastro.entity.UnidadeVenda;
import br.com.seuprojeto.pascoa.cadastro.service.ClienteService;
import br.com.seuprojeto.pascoa.cadastro.service.ProdutoService;
import br.com.seuprojeto.pascoa.common.tenant.TenantContext;
import br.com.seuprojeto.pascoa.orcamento.dto.OrcamentoForm;
import br.com.seuprojeto.pascoa.orcamento.dto.OrcamentoItemForm;
import br.com.seuprojeto.pascoa.orcamento.service.OrcamentoService;
import br.com.seuprojeto.pascoa.pedido.entity.Pedido;
import br.com.seuprojeto.pascoa.pedido.service.PedidoService;
import br.com.seuprojeto.pascoa.producao.repository.OrdemProducaoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class QuantidadeTelasTest {

    @Autowired private MockMvc mvc;
    @Autowired private ClienteService clienteService;
    @Autowired private ProdutoService produtoService;
    @Autowired private PedidoService pedidoService;
    @Autowired private OrcamentoService orcamentoService;
    @Autowired private OrdemProducaoRepository ordens;

    @Test
    @WithMockUser(roles = "ADMIN")
    void telasMostramQuantidadeDecimalComUnidade() throws Exception {
        Object[] ids = TenantContext.calcular(1L, () -> {
            Cliente cliente = clienteService.salvar(Cliente.builder().nome("Cli-" + UUID.randomUUID())
                .telefone("11999990000").build());
            Produto produto = produtoService.salvar(Produto.builder().nome("Bolo-" + UUID.randomUUID())
                .precoVenda(new BigDecimal("40.00")).unidadeVenda(UnidadeVenda.KG).build());
            Pedido pedido = pedidoService.criarComItens(cliente.getId(), null, null, null,
                List.of(produto.getId()), List.of(new BigDecimal("1.5")));
            pedidoService.confirmar(pedido.getId());
            OrcamentoForm form = new OrcamentoForm();
            form.setClienteId(cliente.getId());
            form.setValidade(java.time.LocalDate.now().plusDays(7));
            OrcamentoItemForm item = new OrcamentoItemForm();
            item.setProdutoId(produto.getId());
            item.setQuantidade(new BigDecimal("1.5"));
            item.setPrecoUnitario(new BigDecimal("40.00"));
            form.setItens(List.of(item));
            var orc = orcamentoService.criar(form, "teste");
            Long ordemId = ordens.findByPedidoId(pedido.getId()).get(0).getId();
            return new Object[] {pedido.getId(), pedido.getTokenAcompanhamento(), orc.getId(), orc.getTokenAprovacao(), ordemId};
        });

        mvc.perform(get("/pedidos/" + ids[0])).andExpect(status().isOk())
            .andExpect(content().string(containsString("1,5 kg")));
        mvc.perform(get("/acompanhamento/" + ids[1])).andExpect(status().isOk())
            .andExpect(content().string(containsString("1,5 kg")));
        mvc.perform(get("/orcamentos/" + ids[2])).andExpect(status().isOk())
            .andExpect(content().string(containsString("1,5 kg")));
        mvc.perform(get("/orcamentos/" + ids[2] + "/editar")).andExpect(status().isOk());
        mvc.perform(get("/orcamentos/" + ids[2] + "/pdf")).andExpect(status().isOk());
        mvc.perform(get("/orcamento-publico/" + ids[3])).andExpect(status().isOk())
            .andExpect(content().string(containsString("1,5 kg")));
        mvc.perform(get("/producao/" + ids[4])).andExpect(status().isOk())
            .andExpect(content().string(containsString("1,5 kg")));
        mvc.perform(get("/producao/" + ids[4] + "/pdf")).andExpect(status().isOk());
        mvc.perform(get("/producao")).andExpect(status().isOk());
        mvc.perform(get("/producao/kanban")).andExpect(status().isOk());
        mvc.perform(get("/export/pedido/" + ids[0] + "/pdf")).andExpect(status().isOk());
        mvc.perform(get("/export/pedidos/excel")).andExpect(status().isOk());
        mvc.perform(get("/pedidos/wizard")).andExpect(status().isOk())
            .andExpect(content().string(containsString("data-fracionavel=\"true\"")));
        mvc.perform(get("/pedidos/novo")).andExpect(status().isOk());
        mvc.perform(get("/orcamentos/novo")).andExpect(status().isOk());
        mvc.perform(get("/")).andExpect(status().isOk());
    }
}
