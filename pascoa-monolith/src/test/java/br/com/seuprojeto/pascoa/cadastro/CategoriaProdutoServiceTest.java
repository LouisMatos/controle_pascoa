package br.com.seuprojeto.pascoa.cadastro;

import br.com.seuprojeto.pascoa.cadastro.entity.CategoriaProduto;
import br.com.seuprojeto.pascoa.cadastro.service.CategoriaProdutoService;
import br.com.seuprojeto.pascoa.common.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class CategoriaProdutoServiceTest {

    @Autowired private CategoriaProdutoService service;

    private CategoriaProduto form(String nome) {
        CategoriaProduto c = new CategoriaProduto();
        c.setNome(nome);
        return c;
    }

    @Test
    void salvar_aparaOsEspacos_eRejeitaDuplicataIgnorandoCaixa() {
        String base = "Doces-" + UUID.randomUUID();
        CategoriaProduto salva = TenantContext.calcular(1L, () -> service.salvar(form("  " + base + "  ")));

        assertThat(salva.getNome()).isEqualTo(base);
        assertThatThrownBy(() -> TenantContext.executar(1L, () -> service.salvar(form(base.toUpperCase()))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Já existe uma categoria com este nome.");
    }

    @Test
    void editar_semMudarONome_naoContaComoDuplicata() {
        String nome = "Bolos-" + UUID.randomUUID();
        CategoriaProduto salva = TenantContext.calcular(1L, () -> service.salvar(form(nome)));

        CategoriaProduto edicao = form(nome);
        edicao.setId(salva.getId());
        CategoriaProduto editada = TenantContext.calcular(1L, () -> service.salvar(edicao));

        assertThat(editada.getId()).isEqualTo(salva.getId());
    }

    @Test
    void alternarAtivo_inativaEReativa_eInativaSaiDaListaDeAtivas() {
        String nome = "Tortas-" + UUID.randomUUID();
        Long id = TenantContext.calcular(1L, () -> service.salvar(form(nome)).getId());

        TenantContext.executar(1L, () -> service.alternarAtivo(id));
        assertThat(TenantContext.calcular(1L, () -> service.listarAtivas()))
            .noneMatch(c -> c.getId().equals(id));
        assertThat(TenantContext.calcular(1L, () -> service.listarTodas()))
            .anyMatch(c -> c.getId().equals(id) && !c.getAtivo());

        TenantContext.executar(1L, () -> service.alternarAtivo(id));
        assertThat(TenantContext.calcular(1L, () -> service.listarAtivas()))
            .anyMatch(c -> c.getId().equals(id));
    }
}
