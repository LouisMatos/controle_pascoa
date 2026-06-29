package br.com.seuprojeto.pascoa.tenant.config;

import br.com.seuprojeto.pascoa.commons.events.TenantConfigAlteradoEvent;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MS-01 — declara o exchange de eventos de configuração de tenant.
 * Os consumers (em outros serviços) declaram as próprias queues e
 * fazem o bind ao exchange + routing key.
 */
@Configuration
public class RabbitTenantConfig {

    @Bean
    public TopicExchange tenantConfigExchange() {
        return new TopicExchange(TenantConfigAlteradoEvent.EXCHANGE, true, false);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
