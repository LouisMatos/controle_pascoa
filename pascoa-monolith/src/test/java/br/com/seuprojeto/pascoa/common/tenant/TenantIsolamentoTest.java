package br.com.seuprojeto.pascoa.common.tenant;

import br.com.seuprojeto.pascoa.cadastro.entity.Categoria;
import br.com.seuprojeto.pascoa.cadastro.entity.Cliente;
import br.com.seuprojeto.pascoa.cadastro.entity.Fornecedor;
import br.com.seuprojeto.pascoa.cadastro.entity.PreferenciaCanal;
import br.com.seuprojeto.pascoa.cadastro.entity.Produto;
import br.com.seuprojeto.pascoa.cadastro.repository.ClienteRepository;
import br.com.seuprojeto.pascoa.cadastro.repository.FornecedorRepository;
import br.com.seuprojeto.pascoa.cadastro.repository.ProdutoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class TenantIsolamentoTest {

    @Autowired private ClienteRepository clientes;
    @Autowired private FornecedorRepository fornecedores;
    @Autowired private ProdutoRepository produtos;

    private Cliente novoCliente(String nome) {
        return Cliente.builder().nome(nome).optIn(false).preferenciaCanal(PreferenciaCanal.NENHUM).build();
    }

    private List<String> nomesDeClientes(long loja) {
        return TenantContext.calcular(loja, () -> clientes.findAll().stream().map(Cliente::getNome).toList());
    }

    @Test
    void cliente_daLojaA_naoApareceNaLojaB() {
        String nome = "Cliente-" + UUID.randomUUID();
        TenantContext.executar(1L, () -> clientes.save(novoCliente(nome)));

        assertThat(nomesDeClientes(1L)).contains(nome);
        assertThat(nomesDeClientes(2L)).doesNotContain(nome);
    }

    @Test
    void findById_deOutraLoja_voltaVazio() {
        Long id = TenantContext.calcular(1L, () -> clientes.save(novoCliente("Cliente-" + UUID.randomUUID())).getId());

        assertThat(TenantContext.calcular(1L, () -> clientes.findById(id))).isPresent();
        assertThat(TenantContext.calcular(2L, () -> clientes.findById(id))).isEmpty();
    }

    @Test
    void insert_gravaLojaDoContexto() {
        Cliente salvo = TenantContext.calcular(2L, () -> clientes.save(novoCliente("Cliente-" + UUID.randomUUID())));

        assertThat(salvo.getLojaId()).isEqualTo(2L);
    }

    @Test
    void fornecedorEProduto_tambemSaoIsolados() {
        String nomeFornecedor = "Fornecedor-" + UUID.randomUUID();
        String nomeProduto = "Produto-" + UUID.randomUUID();
        TenantContext.executar(1L, () -> {
            fornecedores.save(Fornecedor.builder().nome(nomeFornecedor).build());
            produtos.save(Produto.builder().nome(nomeProduto).categoria(Categoria.TRUFADO)
                .precoVenda(BigDecimal.TEN).build());
        });

        assertThat(TenantContext.calcular(2L, () -> fornecedores.findAll()))
            .noneMatch(f -> f.getNome().equals(nomeFornecedor));
        assertThat(TenantContext.calcular(2L, () -> produtos.findAll()))
            .noneMatch(p -> p.getNome().equals(nomeProduto));
        assertThat(TenantContext.calcular(1L, () -> produtos.findAll()))
            .anyMatch(p -> p.getNome().equals(nomeProduto));
    }

    @Test
    void semTenant_leituraVazia_escritaFalha() {
        String nome = "Cliente-" + UUID.randomUUID();
        TenantContext.executar(1L, () -> clientes.save(novoCliente(nome)));

        TenantContext.limpar();
        assertThat(clientes.findAll()).isEmpty();
        assertThatThrownBy(() -> clientes.save(novoCliente("Sem-loja-" + UUID.randomUUID())))
            .hasStackTraceContaining("Escrita sem loja no contexto");
    }

    @Test
    void deleteById_deOutraLoja_naoApaga_eGetReferenceByIdFalha() {
        Long id = TenantContext.calcular(1L, () -> fornecedores.save(Fornecedor.builder()
            .nome("Fornecedor-" + UUID.randomUUID()).build()).getId());

        TenantContext.executar(2L, () -> fornecedores.deleteById(id));

        assertThat(TenantContext.calcular(1L, () -> fornecedores.findById(id))).isPresent();
        assertThatThrownBy(() -> TenantContext.executar(2L, () -> fornecedores.getReferenceById(id)))
            .isInstanceOf(org.springframework.orm.jpa.JpaObjectRetrievalFailureException.class)
            .hasRootCauseInstanceOf(jakarta.persistence.EntityNotFoundException.class);
    }

    @Test
    void saveComIdDeOutraLoja_falha_eNaoSobrescreve() {
        Long id = TenantContext.calcular(1L, () -> fornecedores.save(Fornecedor.builder()
            .nome("Fornecedor-" + UUID.randomUUID()).build()).getId());
        String nomeOriginal = TenantContext.calcular(1L, () -> fornecedores.findById(id).orElseThrow().getNome());

        assertThatThrownBy(() -> TenantContext.executar(2L,
            () -> fornecedores.save(Fornecedor.builder().id(id).nome("hack").build())))
            .hasStackTraceContaining("EntityNotFoundException");

        assertThat(TenantContext.calcular(1L, () -> fornecedores.findById(id).orElseThrow().getNome()))
            .isEqualTo(nomeOriginal);
    }

    @Test
    void deleteDeOutraLoja_falha_eMantemALinha() {
        Fornecedor f = TenantContext.calcular(1L, () -> fornecedores.save(Fornecedor.builder()
            .nome("Fornecedor-" + UUID.randomUUID()).build()));

        assertThatThrownBy(() -> TenantContext.executar(2L, () -> fornecedores.delete(f)))
            .hasStackTraceContaining("EntityNotFoundException");

        assertThat(TenantContext.calcular(1L, () -> fornecedores.findById(f.getId()))).isPresent();
    }

    @Test
    void saveDeEntidadeDaLojaAtual_atualiza() {
        Fornecedor f = TenantContext.calcular(1L, () -> fornecedores.save(Fornecedor.builder()
            .nome("Fornecedor-" + UUID.randomUUID()).build()));

        TenantContext.executar(1L, () -> fornecedores.save(Fornecedor.builder().id(f.getId()).nome("novo").build()));

        assertThat(TenantContext.calcular(1L, () -> fornecedores.findById(f.getId()).orElseThrow().getNome()))
            .isEqualTo("novo");
    }
}
