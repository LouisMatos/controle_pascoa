package br.com.seuprojeto.pascoa.cadastro.controller;

import br.com.seuprojeto.pascoa.cadastro.entity.Cliente;
import br.com.seuprojeto.pascoa.cadastro.entity.PreferenciaCanal;
import br.com.seuprojeto.pascoa.cadastro.entity.Produto;
import br.com.seuprojeto.pascoa.cadastro.repository.ClienteRepository;
import br.com.seuprojeto.pascoa.cadastro.repository.ProdutoRepository;
import br.com.seuprojeto.pascoa.pedido.entity.Pedido;
import br.com.seuprojeto.pascoa.pedido.repository.PedidoRepository;
import br.com.seuprojeto.pascoa.pedido.service.PedidoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "ADMIN")
class PainelDoDiaTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private PedidoService pedidoService;
    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private ProdutoRepository produtoRepository;

    private Cliente cliente;
    private Produto produto;

    @BeforeEach
    void setUp() {
        cliente = clienteRepository.save(Cliente.builder()
                .nome("Cliente Painel")
                .optIn(false)
                .preferenciaCanal(PreferenciaCanal.NENHUM)
                .build());
        produto = produtoRepository.save(Produto.builder()
                .nome("Ovo Painel")
                .precoVenda(new BigDecimal("50.00"))
                .ativo(true)
                .build());
    }

    private Pedido pedidoConfirmado(LocalDate dataEntrega) {
        // o serviço recusa entrega no passado; pedido atrasado é criado no futuro e recuado direto na entidade
        LocalDate entregaValida = dataEntrega.isBefore(LocalDate.now()) ? LocalDate.now().plusDays(1) : dataEntrega;
        Pedido pedido = pedidoService.criarComItens(
                cliente.getId(), entregaValida, null, null, List.of(produto.getId()), List.of(1));
        Pedido confirmado = pedidoService.confirmar(pedido.getId());
        if (!entregaValida.equals(dataEntrega)) {
            Pedido atrasado = pedidoRepository.findById(confirmado.getId()).orElseThrow();
            atrasado.setDataEntrega(dataEntrega);
            return pedidoRepository.save(atrasado);
        }
        return confirmado;
    }

    @Test
    @DisplayName("Pedido com entrega hoje aparece em entregarHoje e ordem aberta em produzir")
    void entregaHoje_apareceNoPainel() throws Exception {
        Pedido pedido = pedidoConfirmado(LocalDate.now());

        mockMvc.perform(get("/"))
               .andExpect(status().isOk())
               .andExpect(content().string(org.hamcrest.Matchers.containsString("Entregar hoje")))
               .andExpect(model().attribute("entregarHoje",
                       org.hamcrest.Matchers.hasItem(
                               org.hamcrest.Matchers.hasProperty("id", org.hamcrest.Matchers.is(pedido.getId())))))
               .andExpect(model().attribute("produzir", org.hamcrest.Matchers.not(org.hamcrest.Matchers.empty())));
    }

    @Test
    @DisplayName("Pedido com entrega vencida aparece em atrasados")
    void entregaVencida_apareceEmAtrasados() throws Exception {
        Pedido pedido = pedidoConfirmado(LocalDate.now().minusDays(2));

        mockMvc.perform(get("/"))
               .andExpect(status().isOk())
               .andExpect(model().attribute("atrasados",
                       org.hamcrest.Matchers.hasItem(
                               org.hamcrest.Matchers.hasProperty("id", org.hamcrest.Matchers.is(pedido.getId())))));
    }
}
