package br.com.seuprojeto.pascoa.cadastro;

import br.com.seuprojeto.pascoa.cadastro.entity.Produto;
import br.com.seuprojeto.pascoa.cadastro.entity.UnidadeVenda;
import br.com.seuprojeto.pascoa.cadastro.service.ProdutoService;
import br.com.seuprojeto.pascoa.common.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ProdutoUnidadeSazonalTest {

    @Autowired private ProdutoService service;

    @Test
    void produtoNovo_nasceUnidadeNaoSazonal() {
        Produto salvo = TenantContext.calcular(1L, () -> service.salvar(
            Produto.builder().nome("P-" + UUID.randomUUID()).precoVenda(BigDecimal.TEN).build()));

        assertThat(salvo.getUnidadeVenda()).isEqualTo(UnidadeVenda.UNIDADE);
        assertThat(salvo.getSazonal()).isFalse();
    }

    @Test
    void salvarSazonalComUnidadeCento_persisteOsCampos() {
        LocalDate inicio = LocalDate.of(2026, 3, 1);
        Long id = TenantContext.calcular(1L, () -> service.salvar(Produto.builder()
            .nome("P-" + UUID.randomUUID()).precoVenda(BigDecimal.TEN)
            .unidadeVenda(UnidadeVenda.CENTO).sazonal(true).inicioSafra(inicio).build()).getId());

        Produto lido = TenantContext.calcular(1L, () -> service.buscarPorId(id));

        assertThat(lido.getUnidadeVenda()).isEqualTo(UnidadeVenda.CENTO);
        assertThat(lido.getSazonal()).isTrue();
        assertThat(lido.getInicioSafra()).isEqualTo(inicio);
    }

    @Test
    void naoSazonal_limpaAsDatasDeTemporada() {
        Long id = TenantContext.calcular(1L, () -> service.salvar(Produto.builder()
            .nome("P-" + UUID.randomUUID()).precoVenda(BigDecimal.TEN)
            .sazonal(false).inicioSafra(LocalDate.of(2026, 3, 1)).fimSafra(LocalDate.of(2026, 4, 30)).build()).getId());

        Produto lido = TenantContext.calcular(1L, () -> service.buscarPorId(id));

        assertThat(lido.getInicioSafra()).isNull();
        assertThat(lido.getFimSafra()).isNull();
    }
}
