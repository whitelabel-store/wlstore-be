package com.wlstore.common.outbox;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Publica UM evento da outbox no RabbitMQ e só retorna depois que o broker confirmou (ack).
 * Qualquer falha vira exceção: o evento continua pendente e será tentado de novo.
 * O exchange de destino é do serviço (wlstore.outbox.exchange); o routing key é o tipo do evento.
 */
public class OutboxEventPublisher {
    private static final long CONFIRM_TIMEOUT_SECONDS = 5;

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;

    public OutboxEventPublisher(RabbitTemplate rabbitTemplate, String exchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
    }

    public void publish(OutboxEvent event) {
        String eventId = event.getId().toString();

        Message message = MessageBuilder
                .withBody(event.getPayload().getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setContentEncoding(StandardCharsets.UTF_8.name())
                .setMessageId(eventId)
                .setHeader("tenantId", event.getTenantId().toString())
                .build();

        CorrelationData correlation = new CorrelationData(eventId);
        rabbitTemplate.send(exchange, event.getEventType(), message, correlation);

        awaitConfirm(correlation, eventId);
    }

    private void awaitConfirm(CorrelationData correlation, String eventId) {
        try {
            CorrelationData.Confirm confirm = correlation.getFuture().get(CONFIRM_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!confirm.ack()) {
                throw new IllegalStateException("Broker nack for outbox event " + eventId + ": " + confirm.reason());
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting broker confirm for outbox event " + eventId, ex);
        } catch (ExecutionException | TimeoutException ex) {
            throw new IllegalStateException("No broker confirm for outbox event " + eventId, ex);
        }
    }
}
