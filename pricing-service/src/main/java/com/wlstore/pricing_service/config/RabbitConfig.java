package com.wlstore.pricing_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String CATALOG_EXCHANGE = "catalog.events";
    public static final String CATALOG_EVENTS_QUEUE = "pricing.catalog-events";
    public static final String CATALOG_EVENTS_DLQ = "pricing.catalog-events.dlq";
    public static final String PRODUCT_ROUTING_PATTERN = "product.*";
    public static final String PRICING_EVENTS_EXCHANGE = "pricing.events";

    @Bean
    TopicExchange catalogExchange() {
        return new TopicExchange(CATALOG_EXCHANGE, true, false);
    }

    /** Exchange onde o pricing publica os próprios eventos (price.changed). Topic e durável. */
    @Bean
    TopicExchange pricingEventsExchange() {
        return new TopicExchange(PRICING_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    Queue catalogEventsQueue() {
        return QueueBuilder.durable(CATALOG_EVENTS_QUEUE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(CATALOG_EVENTS_DLQ)
                .build();
    }

    @Bean
    Queue catalogEventsDeadLetterQueue() {
        return QueueBuilder.durable(CATALOG_EVENTS_DLQ).build();
    }

    @Bean
    Binding catalogEventsBinding() {
        return BindingBuilder.bind(catalogEventsQueue())
                .to(catalogExchange())
                .with(PRODUCT_ROUTING_PATTERN);
    }
}
