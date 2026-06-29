package br.com.seuprojeto.pascoa.commons.tenant;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * MS-02 — copia o {@link TenantContext} (ThreadLocal) da thread requisitante
 * para a thread do pool ao agendar um {@code @Async} ou {@code @Scheduled}.
 *
 * Sem este decorator, `TenantContext.get()` dentro de métodos assíncronos
 * cai para o {@code DEFAULT_TENANT} ("public") — o que pode silenciosamente
 * vazar dados entre tenants em jobs disparados a partir de uma requisição
 * autenticada de tenant A.
 *
 * Também propaga o MDC (logs) para que o tenant correto apareça em traces.
 */
public class TenantAwareTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // Capturados na thread pai, no momento do .submit/.execute.
        String tenantId = TenantContext.getOrNull();
        Map<String, String> mdc = MDC.getCopyOfContextMap();

        return () -> {
            String previousTenant = TenantContext.getOrNull();
            Map<String, String> previousMdc = MDC.getCopyOfContextMap();
            try {
                if (tenantId != null) TenantContext.set(tenantId); else TenantContext.clear();
                if (mdc != null) MDC.setContextMap(mdc); else MDC.clear();
                runnable.run();
            } finally {
                // Restaura o estado anterior do worker (pool reusa thread).
                if (previousTenant != null) TenantContext.set(previousTenant); else TenantContext.clear();
                if (previousMdc != null) MDC.setContextMap(previousMdc); else MDC.clear();
            }
        };
    }
}
