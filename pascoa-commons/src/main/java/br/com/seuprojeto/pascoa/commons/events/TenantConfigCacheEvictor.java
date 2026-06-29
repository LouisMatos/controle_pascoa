package br.com.seuprojeto.pascoa.commons.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;

/**
 * MS-01 — listener opt-in que escuta {@link TenantConfigAlteradoEvent}
 * e faz `cacheManager.getCache(...).evict(tenantId)` nos caches
 * configurados.
 *
 * Ativação no microsserviço consumidor (application.yml):
 *   foodflow.tenant-config-cache-evictor.enabled: true
 *   foodflow.tenant-config-cache-evictor.caches: tenant-config,white-label,feature-flags
 *
 * Cada serviço deve declarar/auto-criar uma queue com nome único
 * (já feito aqui via spring.application.name na binding) e o
 * CacheManager + @EnableCaching no contexto.
 */
@AutoConfiguration
@ConditionalOnClass({ CacheManager.class, RabbitListener.class })
@ConditionalOnProperty(prefix = "foodflow.tenant-config-cache-evictor",
                       name = "enabled", havingValue = "true")
public class TenantConfigCacheEvictor {

    @Bean
    public TopicExchange tenantConfigExchangeForEvictor() {
        return new TopicExchange(TenantConfigAlteradoEvent.EXCHANGE, true, false);
    }

    @Bean
    public Queue tenantConfigEvictorQueue(@Value("${spring.application.name:app}") String app) {
        // Queue por serviço para garantir fan-out (cada consumidor recebe sua cópia).
        return new Queue("config.tenant.alterado." + app + ".queue", true);
    }

    @Bean
    public Binding tenantConfigEvictorBinding(Queue tenantConfigEvictorQueue,
                                              TopicExchange tenantConfigExchangeForEvictor) {
        return BindingBuilder.bind(tenantConfigEvictorQueue)
                .to(tenantConfigExchangeForEvictor)
                .with(TenantConfigAlteradoEvent.ROUTING_KEY);
    }

    @Bean
    public Listener tenantConfigCacheEvictorListener(
            CacheManager cacheManager,
            @Value("${foodflow.tenant-config-cache-evictor.caches:tenant-config}") String caches) {
        return new Listener(cacheManager, caches.split("\\s*,\\s*"));
    }

    @Slf4j
    @RequiredArgsConstructor
    public static class Listener {
        private final CacheManager cacheManager;
        private final String[] cacheNames;

        @RabbitListener(queues = "#{tenantConfigEvictorQueue.name}")
        public void onTenantConfigChanged(TenantConfigAlteradoEvent ev) {
            for (String cache : cacheNames) {
                var c = cacheManager.getCache(cache);
                if (c != null) {
                    c.evict(ev.getTenantId());
                    log.debug("Cache '{}' evicted para tenant {}", cache, ev.getTenantId());
                }
            }
        }
    }
}
