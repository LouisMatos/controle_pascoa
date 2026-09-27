package br.com.seuprojeto.pascoa.production.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String QUEUE_ORDER_CONFIRMED = "pascoa.production.order-confirmed";
    private static final String EXCHANGE_ORDERS = "pascoa.orders";

    @Bean
    public TopicExchange ordersExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE_ORDERS).durable(true).build();
    }

    @Bean
    public Queue orderConfirmedQueue() {
        return QueueBuilder.durable(QUEUE_ORDER_CONFIRMED)
                .withArgument("x-dead-letter-exchange", "pascoa.dlx")
                .withArgument("x-dead-letter-routing-key", QUEUE_ORDER_CONFIRMED + ".dlq")
                .build();
    }

    @Bean
    public Binding orderConfirmedBinding(Queue orderConfirmedQueue, TopicExchange ordersExchange) {
        return BindingBuilder.bind(orderConfirmedQueue).to(ordersExchange).with("order.confirmed");
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
