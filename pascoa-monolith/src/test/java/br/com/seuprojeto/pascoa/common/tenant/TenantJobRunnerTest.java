package br.com.seuprojeto.pascoa.common.tenant;

import br.com.seuprojeto.pascoa.seguranca.entity.Loja;
import br.com.seuprojeto.pascoa.seguranca.repository.LojaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class TenantJobRunnerTest {

    @Autowired private TenantJobRunner runner;
    @Autowired private LojaRepository lojas;
    @Autowired private AsyncTaskExecutor applicationTaskExecutor;

    @Test
    void porLoja_executaUmaVezPorLojaNoTenantCerto_eUmaFalhaNaoParaAsOutras() {
        Long a = lojas.save(Loja.builder().nome("A").build()).getId();
        Long b = lojas.save(Loja.builder().nome("B").build()).getId();
        List<Long> visitadas = new ArrayList<>();

        runner.porLoja(() -> {
            visitadas.add(TenantContext.atual());
            if (TenantContext.atual() == a) {
                throw new IllegalStateException("falha na primeira");
            }
        });

        assertThat(visitadas).contains(a, b);
        assertThat(TenantContext.atual()).isEqualTo(1L);
    }

    @Test
    void executorAsync_propagaOTenantDaThreadQueSubmete() throws Exception {
        long visto = TenantContext.calcular(5L, () -> {
            try {
                return applicationTaskExecutor.submit(TenantContext::atual).get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });

        assertThat(visto).isEqualTo(5L);
    }
}
