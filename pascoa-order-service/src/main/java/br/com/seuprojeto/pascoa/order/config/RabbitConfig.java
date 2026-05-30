package br.com.seuprojeto.pascoa.order.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String QUEUE_PRODUCTION_COMPLETED = "pascoa.order.production-completed";
    private static final String EXCHANGE_PRODUCTION = "pascoa.production";

    @Bean
    public TopicExchange productionExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE_PRODUCTION).durable(true).build();
    }

    @Bean
    public Queue productionCompletedQueue() {
        return QueueBuilder.durable(QUEUE_PRODUCTION_COMPLETED)
                .withArgument("x-dead-letter-exchange", "pascoa.dlx")
                .withArgument("x-dead-letter-routing-key", QUEUE_PRODUCTION_COMPLETED + ".dlq")
                .build();
    }

    @Bean
    public Binding productionCompletedBinding(Queue productionCompletedQueue,
                                               TopicExchange productionExchange) {
        return BindingBuilder.bind(productionCompletedQueue)
                .to(productionExchange).with("production.completed");
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        var factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
