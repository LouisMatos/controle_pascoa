package br.com.seuprojeto.pascoa.orcamento.service;

import br.com.seuprojeto.pascoa.cadastro.entity.Cliente;
import br.com.seuprojeto.pascoa.cadastro.entity.Produto;
import br.com.seuprojeto.pascoa.orcamento.entity.Orcamento;
import br.com.seuprojeto.pascoa.orcamento.entity.OrcamentoItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrcamentoPdfServiceTest {

    @Test
    void gerar_produzPdfValido() {
        Cliente cliente = Cliente.builder()
                .nome("Maria")
                .telefone("11999990000")
                .email("maria@example.com")
                .build();

        OrcamentoItem item = OrcamentoItem.builder()
                .produto(Produto.builder().nome("Ovo 500g").build())
                .quantidade(new BigDecimal("2"))
                .precoUnitario(new BigDecimal("80.00"))
                .subtotal(new BigDecimal("160.00"))
                .build();

        Orcamento orc = Orcamento.builder()
                .id(1L)
                .cliente(cliente)
                .dataCriacao(LocalDateTime.now())
                .validade(LocalDate.now().plusDays(7))
                .total(new BigDecimal("160.00"))
                .observacoes("Entregar refrigerado.")
                .tokenAprovacao("token-abc")
                .itens(List.of(item))
                .build();

        byte[] pdf = new OrcamentoPdfService().gerar(orc);

        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        assertThat(pdf.length).isGreaterThan(1000);
    }
}
