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
import br.com.seuprojeto.pascoa.qualidade.service.QualidadeService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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
    @Autowired private QualidadeService qualidadeService;

    private Long[] pedidoConfirmado(Cliente cliente, Produto produto, String qtd) {
        Pedido pedido = pedidoService.criarComItens(cliente.getId(), null, null, null,
            List.of(produto.getId()), List.of(new BigDecimal(qtd)));
        pedidoService.confirmar(pedido.getId());
        return new Long[] {pedido.getId(), ordens.findByPedidoId(pedido.getId()).get(0).getId()};
    }

    private void pdf(String url) throws Exception {
        byte[] corpo = mvc.perform(get(url)).andExpect(status().isOk())
            .andExpect(content().contentType("application/pdf"))
            .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(corpo, 0, 4, java.nio.charset.StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void qualidadeFinanceiroEAnalytics_mostramQuantidadeFormatada() throws Exception {
        Long[] ids = TenantContext.calcular(1L, () -> {
            Cliente cliente = clienteService.salvar(Cliente.builder().nome("Cli-" + UUID.randomUUID())
                .telefone("11999990000").build());
            Produto produto = produtoService.salvar(Produto.builder().nome("Queijo-" + UUID.randomUUID())
                .precoVenda(new BigDecimal("40.00")).unidadeVenda(UnidadeVenda.KG).build());
            Long[] inspecionado = pedidoConfirmado(cliente, produto, "2.25");
            Long inspecaoId = qualidadeService.registrarInspecao(inspecionado[1], "Insp", true, null, null).getId();
            Long[] semInspecao = pedidoConfirmado(cliente, produto, "3.5");
            Long[] grande = pedidoConfirmado(cliente, produto, "7777.5");
            return new Long[] {inspecaoId, semInspecao[1], grande[0]};
        });

        mvc.perform(get("/qualidade/inspecao/" + ids[0])).andExpect(status().isOk())
            .andExpect(content().string(containsString("2,25 kg")));
        mvc.perform(get("/qualidade/inspecao/nova/" + ids[1])).andExpect(status().isOk())
            .andExpect(content().string(containsString("3,5 kg")));
        mvc.perform(get("/financeiro/custo-real/" + ids[2])).andExpect(status().isOk())
            .andExpect(content().string(containsString("7777,5")));
        mvc.perform(get("/financeiro/dashboard")).andExpect(status().isOk())
            .andExpect(content().string(containsString("7783,25")));
        mvc.perform(get("/analytics")).andExpect(status().isOk())
            .andExpect(content().string(containsString("7783,25")));
    }

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
        pdf("/orcamentos/" + ids[2] + "/pdf");
        mvc.perform(get("/orcamento-publico/" + ids[3])).andExpect(status().isOk())
            .andExpect(content().string(containsString("1,5 kg")));
        mvc.perform(get("/producao/" + ids[4])).andExpect(status().isOk())
            .andExpect(content().string(containsString("1,5 kg")))
            .andExpect(content().string(not(containsString("unidade(s)"))));
        pdf("/producao/" + ids[4] + "/pdf");
        mvc.perform(get("/producao")).andExpect(status().isOk());
        mvc.perform(get("/producao/kanban")).andExpect(status().isOk())
            .andExpect(content().string(not(containsString("unid."))));
        pdf("/export/pedido/" + ids[0] + "/pdf");
        byte[] excel = mvc.perform(get("/export/pedidos/excel")).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsByteArray();
        assertThat(excel).isNotEmpty();
        mvc.perform(get("/pedidos/wizard")).andExpect(status().isOk())
            .andExpect(content().string(containsString("data-fracionavel=\"true\"")));
        mvc.perform(get("/pedidos/novo")).andExpect(status().isOk());
        mvc.perform(get("/orcamentos/novo")).andExpect(status().isOk());
        mvc.perform(get("/")).andExpect(status().isOk());
    }
}
