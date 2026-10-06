package br.com.seuprojeto.pascoa.cadastro;

import br.com.seuprojeto.pascoa.cadastro.entity.CategoriaProduto;
import br.com.seuprojeto.pascoa.cadastro.entity.Produto;
import br.com.seuprojeto.pascoa.cadastro.repository.CategoriaProdutoRepository;
import br.com.seuprojeto.pascoa.cadastro.service.ProdutoService;
import br.com.seuprojeto.pascoa.common.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class CategoriaProdutoTest {

    @Autowired private CategoriaProdutoRepository categorias;
    @Autowired private ProdutoService produtoService;

    private CategoriaProduto nova(String nome) {
        return CategoriaProduto.builder().nome(nome).build();
    }

    private List<String> nomes(long loja) {
        return TenantContext.calcular(loja, () -> categorias.findAll().stream().map(CategoriaProduto::getNome).toList());
    }

    @Test
    void categoria_daLojaA_naoApareceNaLojaB() {
        String nome = "Cat-" + UUID.randomUUID();
        TenantContext.executar(1L, () -> categorias.save(nova(nome)));

        assertThat(nomes(1L)).contains(nome);
        assertThat(nomes(2L)).doesNotContain(nome);
    }

    @Test
    void mesmoNome_emLojasDiferentes_epermitido_emDuplicidadeNaMesmaLoja_falha() {
        String nome = "Cat-" + UUID.randomUUID();
        TenantContext.executar(1L, () -> categorias.save(nova(nome)));
        TenantContext.executar(2L, () -> categorias.save(nova(nome)));

        assertThatThrownBy(() -> TenantContext.executar(1L, () -> categorias.saveAndFlush(nova(nome))))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void salvarProduto_comCategoriaDeOutraLoja_falha() {
        Long categoriaDaLoja2 = TenantContext.calcular(2L,
            () -> categorias.save(nova("Cat-" + UUID.randomUUID())).getId());
        Produto produto = Produto.builder().nome("P-" + UUID.randomUUID()).precoVenda(BigDecimal.TEN)
            .categoria(CategoriaProduto.builder().id(categoriaDaLoja2).build()).build();

        assertThatThrownBy(() -> TenantContext.executar(1L, () -> produtoService.salvar(produto)))
            .hasStackTraceContaining("Categoria não encontrada");
    }

    @Test
    void editarProdutoDestacado_mantemLojaCategoriaENome() {
        String sufixo = UUID.randomUUID().toString();
        Long categoriaId = TenantContext.calcular(1L, () -> categorias.save(nova("Cat-" + sufixo)).getId());
        Long produtoId = TenantContext.calcular(1L, () -> produtoService.salvar(
            Produto.builder().nome("A-" + sufixo).precoVenda(BigDecimal.TEN).build()).getId());
        Produto editado = Produto.builder().id(produtoId).nome("B-" + sufixo).precoVenda(BigDecimal.TEN)
            .categoria(CategoriaProduto.builder().id(categoriaId).build()).build();

        TenantContext.executar(1L, () -> produtoService.salvar(editado));
        Produto lido = TenantContext.calcular(1L, () -> produtoService.buscarPorId(produtoId));

        assertThat(lido.getNome()).isEqualTo("B-" + sufixo);
        assertThat(lido.getLojaId()).isEqualTo(1L);
        assertThat(lido.getCategoria().getId()).isEqualTo(categoriaId);
    }

    @Test
    void produtoSemCategoria_epermitido() {
        Produto salvo = TenantContext.calcular(1L, () -> produtoService.salvar(
            Produto.builder().nome("P-" + UUID.randomUUID()).precoVenda(BigDecimal.TEN).build()));

        assertThat(salvo.getCategoria()).isNull();
    }
}
