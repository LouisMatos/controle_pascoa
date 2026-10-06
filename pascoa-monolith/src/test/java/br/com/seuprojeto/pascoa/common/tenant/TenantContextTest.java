package br.com.seuprojeto.pascoa.common.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantContextTest {

    @AfterEach
    void limpar() {
        TenantContext.limpar();
    }

    @Test
    void semContexto_atualDevolveSentinela() {
        assertThat(TenantContext.atual()).isEqualTo(TenantContext.SEM_TENANT);
    }

    @Test
    void semContexto_exigirLanca() {
        assertThatThrownBy(TenantContext::exigir).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void abrir_restauraTenantAnterior() {
        TenantContext.set(1L);
        try (var escopo = TenantContext.abrir(2L)) {
            assertThat(TenantContext.atual()).isEqualTo(2L);
        }
        assertThat(TenantContext.atual()).isEqualTo(1L);
    }

    @Test
    void executar_restauraMesmoComExcecao() {
        TenantContext.set(1L);
        assertThatThrownBy(() -> TenantContext.executar(2L, () -> { throw new IllegalArgumentException("x"); }))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(TenantContext.atual()).isEqualTo(1L);
    }

    @Test
    void calcular_devolveValorNoTenantIndicado() {
        long visto = TenantContext.calcular(3L, TenantContext::atual);
        assertThat(visto).isEqualTo(3L);
        assertThat(TenantContext.atual()).isEqualTo(TenantContext.SEM_TENANT);
    }
}
