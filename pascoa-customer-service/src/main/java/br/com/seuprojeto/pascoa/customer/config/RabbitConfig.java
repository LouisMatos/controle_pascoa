package br.com.seuprojeto.pascoa.customer.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String QUEUE_AUTH_LOGIN = "pascoa.customer.auth-login";
    private static final String EXCHANGE_CUSTOMERS = "pascoa.customers";

    @Bean
    public TopicExchange customersExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE_CUSTOMERS).durable(true).build();
    }

    @Bean
    public Queue authLoginQueue() {
        return QueueBuilder.durable(QUEUE_AUTH_LOGIN)
                .withArgument("x-dead-letter-exchange", "pascoa.dlx")
                .withArgument("x-dead-letter-routing-key", QUEUE_AUTH_LOGIN + ".dlq")
                .build();
    }

    @Bean
    public Binding authLoginBinding(Queue authLoginQueue, TopicExchange customersExchange) {
        return BindingBuilder.bind(authLoginQueue)
                .to(customersExchange)
                .with("auth.login.success");
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
