package br.com.seuprojeto.pascoa.common.tenant;

import br.com.seuprojeto.pascoa.cadastro.entity.Cliente;
import br.com.seuprojeto.pascoa.cadastro.entity.MateriaPrima;
import br.com.seuprojeto.pascoa.cadastro.entity.PreferenciaCanal;
import br.com.seuprojeto.pascoa.cadastro.entity.Unidade;
import br.com.seuprojeto.pascoa.cadastro.repository.ClienteRepository;
import br.com.seuprojeto.pascoa.cadastro.repository.MateriaPrimaRepository;
import br.com.seuprojeto.pascoa.estoque.entity.MovimentacaoEstoque;
import br.com.seuprojeto.pascoa.estoque.entity.TipoMovimentacao;
import br.com.seuprojeto.pascoa.estoque.repository.MovimentacaoEstoqueRepository;
import br.com.seuprojeto.pascoa.gastos.entity.CategoriaGasto;
import br.com.seuprojeto.pascoa.gastos.entity.GastoVariavel;
import br.com.seuprojeto.pascoa.gastos.repository.GastoVariavelRepository;
import br.com.seuprojeto.pascoa.pedido.entity.Pedido;
import br.com.seuprojeto.pascoa.pedido.repository.PedidoRepository;
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
class TenantIsolamentoEntidadesTest {

    @Autowired private ClienteRepository clientes;
    @Autowired private PedidoRepository pedidos;
    @Autowired private MateriaPrimaRepository materiasPrimas;
    @Autowired private MovimentacaoEstoqueRepository movimentacoes;
    @Autowired private GastoVariavelRepository gastos;

    private MateriaPrima novaMateria() {
        return MateriaPrima.builder().nome("MP-" + UUID.randomUUID()).unidade(Unidade.KG).build();
    }

    @Test
    void pedido_somenteDaLojaQueCriou() {
        Pedido pedido = TenantContext.calcular(1L, () -> {
            Cliente c = clientes.save(Cliente.builder().nome("C-" + UUID.randomUUID()).optIn(false)
                .preferenciaCanal(PreferenciaCanal.NENHUM).build());
            return pedidos.save(Pedido.builder().cliente(c).build());
        });
        Long id = pedido.getId();

        assertThat(pedido.getLojaId()).isEqualTo(1L);
        assertThat(TenantContext.calcular(1L, () -> pedidos.findById(id))).isPresent();
        assertThat(TenantContext.calcular(1L, () -> pedidos.findAll())).anyMatch(p -> p.getId().equals(id));
        assertThat(TenantContext.calcular(2L, () -> pedidos.findById(id))).isEmpty();
        assertThat(TenantContext.calcular(2L, () -> pedidos.findAll())).isEmpty();
        assertThat(TenantContext.calcular(2L, () -> pedidos.count())).isZero();
    }

    @Test
    void materiaPrima_leituraEEscritaPorLoja() {
        MateriaPrima mp = TenantContext.calcular(1L, () -> materiasPrimas.save(novaMateria()));
        MateriaPrima mp2 = TenantContext.calcular(2L, () -> materiasPrimas.save(novaMateria()));
        long totalLoja1 = TenantContext.calcular(1L, () -> materiasPrimas.count());

        assertThat(mp.getLojaId()).isEqualTo(1L);
        assertThat(mp2.getLojaId()).isEqualTo(2L);
        assertThat(TenantContext.calcular(2L, () -> materiasPrimas.findById(mp.getId()))).isEmpty();
        assertThat(TenantContext.calcular(2L, () -> materiasPrimas.findAll()))
            .anyMatch(m -> m.getId().equals(mp2.getId())).noneMatch(m -> m.getId().equals(mp.getId()));
        assertThat(TenantContext.calcular(1L, () -> materiasPrimas.findAll()))
            .noneMatch(m -> m.getId().equals(mp2.getId()));
        assertThat(TenantContext.calcular(1L, () -> materiasPrimas.count())).isEqualTo(totalLoja1);
    }

    @Test
    void movimentacaoEstoque_leituraEEscritaPorLoja() {
        MovimentacaoEstoque mov = TenantContext.calcular(2L, () -> {
            MateriaPrima mp = materiasPrimas.save(novaMateria());
            return movimentacoes.save(MovimentacaoEstoque.builder().materiaPrima(mp).tipo(TipoMovimentacao.ENTRADA)
                .quantidade(BigDecimal.ONE).saldoApos(BigDecimal.ONE).build());
        });

        assertThat(mov.getLojaId()).isEqualTo(2L);
        assertThat(TenantContext.calcular(2L, () -> movimentacoes.findById(mov.getId()))).isPresent();
        assertThat(TenantContext.calcular(2L, () -> movimentacoes.findAll()))
            .anyMatch(m -> m.getId().equals(mov.getId()));
        assertThat(TenantContext.calcular(1L, () -> movimentacoes.findById(mov.getId()))).isEmpty();
        assertThat(TenantContext.calcular(1L, () -> movimentacoes.findAll()))
            .noneMatch(m -> m.getId().equals(mov.getId()));
    }

    @Test
    void gastoVariavel_leituraEEscritaPorLoja() {
        GastoVariavel gasto = TenantContext.calcular(2L, () -> gastos.save(GastoVariavel.builder()
            .descricao("G-" + UUID.randomUUID()).valor(BigDecimal.TEN).dataLancamento(LocalDate.now())
            .categoria(CategoriaGasto.OUTROS).build()));
        long totalLoja1 = TenantContext.calcular(1L, () -> gastos.count());

        assertThat(gasto.getLojaId()).isEqualTo(2L);
        assertThat(TenantContext.calcular(2L, () -> gastos.findById(gasto.getId()))).isPresent();
        assertThat(TenantContext.calcular(1L, () -> gastos.findById(gasto.getId()))).isEmpty();
        assertThat(TenantContext.calcular(1L, () -> gastos.findAll()))
            .noneMatch(g -> g.getId().equals(gasto.getId()));
        assertThat(TenantContext.calcular(1L, () -> gastos.count())).isEqualTo(totalLoja1);
    }
}
