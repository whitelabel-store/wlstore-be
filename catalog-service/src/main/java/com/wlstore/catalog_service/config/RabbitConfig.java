package com.wlstore.catalog_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String CATALOG_EVENTS_EXCHANGE = "catalog.events";

    public static final String PRICING_EVENTS_EXCHANGE = "pricing.events";
    public static final String PRICING_EVENTS_QUEUE = "catalog.pricing-events";
    public static final String PRICING_EVENTS_DLQ = "catalog.pricing-events.dlq";
    public static final String PRICING_EVENTS_ROUTING_KEY = "price.*";

    /** Exchange do tipo topic, durável: sobrevive a reinício do broker. */
    @Bean
    public TopicExchange catalogEventsExchange() {
        return new TopicExchange(CATALOG_EVENTS_EXCHANGE, true, false);
    }

    /** Quem publica (pricing) também a declara; declarar igual dos dois lados é seguro e tira a dependência de ordem de subida. */
    @Bean
    public TopicExchange pricingEventsExchange() {
        return new TopicExchange(PRICING_EVENTS_EXCHANGE, true, false);
    }

    /** Caixa de correio do catálogo para eventos de preço; mensagem rejeitada vai para a DLQ. */
    @Bean
    public Queue pricingEventsQueue() {
        return QueueBuilder.durable(PRICING_EVENTS_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", PRICING_EVENTS_DLQ)
                .build();
    }

    @Bean
    public Queue pricingEventsDeadLetterQueue() {
        return QueueBuilder.durable(PRICING_EVENTS_DLQ).build();
    }

    @Bean
    public Binding pricingEventsBinding(Queue pricingEventsQueue, TopicExchange pricingEventsExchange) {
        return BindingBuilder.bind(pricingEventsQueue)
                .to(pricingEventsExchange)
                .with(PRICING_EVENTS_ROUTING_KEY);
    }
}