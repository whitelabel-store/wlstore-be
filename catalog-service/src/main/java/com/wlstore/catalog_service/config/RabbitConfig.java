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

    public static final String INVENTORY_EVENTS_EXCHANGE = "inventory.events";
    public static final String INVENTORY_EVENTS_QUEUE = "catalog.inventory-events";
    public static final String INVENTORY_EVENTS_DLQ = "catalog.inventory-events.dlq";
    public static final String INVENTORY_EVENTS_ROUTING_KEY = "stock.*";

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

    /** Mesma regra do pricing: o inventory também declara este exchange. */
    @Bean
    public TopicExchange inventoryEventsExchange() {
        return new TopicExchange(INVENTORY_EVENTS_EXCHANGE, true, false);
    }

    /** Caixa de correio do catálogo para eventos de estoque; mensagem rejeitada vai para a DLQ. */
    @Bean
    public Queue inventoryEventsQueue() {
        return QueueBuilder.durable(INVENTORY_EVENTS_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", INVENTORY_EVENTS_DLQ)
                .build();
    }

    @Bean
    public Queue inventoryEventsDeadLetterQueue() {
        return QueueBuilder.durable(INVENTORY_EVENTS_DLQ).build();
    }

    @Bean
    public Binding inventoryEventsBinding(Queue inventoryEventsQueue, TopicExchange inventoryEventsExchange) {
        return BindingBuilder.bind(inventoryEventsQueue)
                .to(inventoryEventsExchange)
                .with(INVENTORY_EVENTS_ROUTING_KEY);
    }
}