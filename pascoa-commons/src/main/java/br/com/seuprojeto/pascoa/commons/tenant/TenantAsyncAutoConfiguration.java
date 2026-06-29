package br.com.seuprojeto.pascoa.commons.tenant;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * MS-02 — registra um {@link Executor} para {@code @Async} que propaga o
 * {@link TenantContext} via {@link TenantAwareTaskDecorator}.
 *
 * Ativada junto com a infra multi-tenant ({@code foodflow.tenant.enabled=true}).
 * Substitui o `SimpleAsyncTaskExecutor` default do Spring (que não propaga
 * ThreadLocal nem MDC). Se o serviço já declarar um bean {@code taskExecutor},
 * o {@code @ConditionalOnMissingBean} respeita.
 *
 * Pool conservador (compatível com a "Política de Recursos" do CLAUDE.md):
 *   core=2, max=10, queue=100.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "foodflow.tenant", name = "enabled", havingValue = "true")
@EnableAsync
public class TenantAsyncAutoConfiguration {

    @Bean(name = "taskExecutor")
    @ConditionalOnMissingBean(name = "taskExecutor")
    public Executor tenantAwareTaskExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(2);
        exec.setMaxPoolSize(10);
        exec.setQueueCapacity(100);
        exec.setThreadNamePrefix("ff-async-");
        exec.setTaskDecorator(new TenantAwareTaskDecorator());
        exec.initialize();
        return exec;
    }
}
