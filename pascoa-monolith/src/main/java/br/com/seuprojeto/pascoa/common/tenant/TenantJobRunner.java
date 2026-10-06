package br.com.seuprojeto.pascoa.common.tenant;

import br.com.seuprojeto.pascoa.seguranca.entity.Loja;
import br.com.seuprojeto.pascoa.seguranca.repository.LojaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class TenantJobRunner {

    private final LojaRepository lojaRepository;
    private final TransactionTemplate transactionTemplate;

    public void porLoja(Runnable job) {
        for (Loja loja : lojaRepository.findAll()) {
            try (var escopo = TenantContext.abrir(loja.getId())) {
                transactionTemplate.executeWithoutResult(status -> job.run());
            } catch (RuntimeException e) {
                log.error("[TENANT] Job falhou para a loja {}: {}", loja.getId(), e.getMessage(), e);
            }
        }
    }
}
