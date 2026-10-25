package com.wlstore.pricing_service.outbox;

import com.wlstore.pricing_service.AbstractIntegrationTest;
import com.wlstore.pricing_service.service.PriceService;
import com.wlstore.pricing_service.service.SkuSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class OutboxRelayIntegrationTest extends AbstractIntegrationTest {

    private static final String TEST_QUEUE = "test.pricing.events";

    @Autowired
    PriceService priceService;

    @Autowired
    SkuSyncService skuSyncService;

    @Autowired
    OutboxEventRepository outboxEventRepository;

    @Autowired
    OutboxEventPublisher publisher;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    AmqpAdmin amqpAdmin;

    @Autowired
    @Qualifier("pricingEventsExchange")
    TopicExchange pricingEventsExchange;

    @Test
    void changedPriceIsPublishedToRabbitAndMarkedAsPublished() {
        // Sem fila ligada ao exchange o RabbitMQ descarta a mensagem: esta fila faz o papel de um consumidor.
        Queue queue = new Queue(TEST_QUEUE, false, false, true);
        amqpAdmin.declareExchange(pricingEventsExchange);
        amqpAdmin.declareQueue(queue);
        amqpAdmin.declareBinding(BindingBuilder.bind(queue).to(pricingEventsExchange).with("price.*"));
        amqpAdmin.purgeQueue(TEST_QUEUE, false);

        // Eventos pendentes de outros testes ficam no mesmo banco; esta limpeza vale só dentro da transação do teste.
        outboxEventRepository.deleteAll();
        outboxEventRepository.flush();

        skuSyncService.sync(UUID.randomUUID(), Set.of("RLY-001"));
        priceService.definePrice("RLY-001", 9000L, 10000L);

        // O relayer agendado está desligado nos testes; aqui o chamamos na mão, na transação do teste.
        new OutboxRelayer(outboxEventRepository, publisher).relayPending();

        Message message = rabbitTemplate.receive(TEST_QUEUE, 5000);

        assertThat(message).as("mensagem recebida na fila").isNotNull();
        assertThat(message.getMessageProperties().getReceivedRoutingKey()).isEqualTo("price.changed");
        assertThat(new String(message.getBody(), StandardCharsets.UTF_8)).contains("RLY-001");

        List<OutboxEvent> events = outboxEventRepository.findAll();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getPublishedAt()).isNotNull();
        assertThat(message.getMessageProperties().getMessageId()).isEqualTo(events.get(0).getId().toString());
    }
}