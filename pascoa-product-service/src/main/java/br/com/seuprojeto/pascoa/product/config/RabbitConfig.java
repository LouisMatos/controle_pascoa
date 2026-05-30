package br.com.seuprojeto.pascoa.product.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String QUEUE_STOCK_CRITICAL = "pascoa.product.stock-critical";
    private static final String EXCHANGE_PRODUCTION  = "pascoa.production";

    @Bean
    public TopicExchange productionExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE_PRODUCTION).durable(true).build();
    }

    @Bean
    public Queue stockCriticalQueue() {
        return QueueBuilder.durable(QUEUE_STOCK_CRITICAL)
                .withArgument("x-dead-letter-exchange", "pascoa.dlx")
                .withArgument("x-dead-letter-routing-key", QUEUE_STOCK_CRITICAL + ".dlq")
                .build();
    }

    @Bean
    public Binding stockCriticalBinding(Queue stockCriticalQueue, TopicExchange productionExchange) {
        return BindingBuilder.bind(stockCriticalQueue)
                .to(productionExchange).with("inventory.stock.critical");
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
