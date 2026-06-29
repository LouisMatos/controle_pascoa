package br.com.seuprojeto.pascoa.commons.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MS-01 — publisher reutilizável do {@link TenantConfigAlteradoEvent}.
 *
 * Injetar este bean em qualquer service que altere config de tenant e
 * chamar {@link #publicar(String, TenantConfigAlteradoEvent.Tipo)} após
 * o commit da alteração. Os consumers (em outros serviços) invalidarão
 * o cache local correspondente.
 */
@Configuration
@ConditionalOnClass(RabbitTemplate.class)
public class TenantConfigEventPublisher {

    @Slf4j
    @RequiredArgsConstructor
    public static class Publisher {
        private final RabbitTemplate rabbit;

        public void publicar(String tenantId, TenantConfigAlteradoEvent.Tipo tipo) {
            try {
                rabbit.convertAndSend(
                        TenantConfigAlteradoEvent.EXCHANGE,
                        TenantConfigAlteradoEvent.ROUTING_KEY,
                        new TenantConfigAlteradoEvent(tenantId, tipo));
                log.debug("Evento TenantConfigAlteradoEvent publicado — tenant={}, tipo={}", tenantId, tipo);
            } catch (Exception e) {
                // Best-effort: cache local invalidado por @CacheEvict; outros nós
                // expiram pelo TTL. Log para visibilidade sem quebrar a tx.
                log.warn("Falha ao publicar TenantConfigAlteradoEvent para tenant {}: {}",
                        tenantId, e.getMessage());
            }
        }
    }

    @Bean
    public Publisher tenantConfigEventPublisher(RabbitTemplate rabbit) {
        return new Publisher(rabbit);
    }
}
