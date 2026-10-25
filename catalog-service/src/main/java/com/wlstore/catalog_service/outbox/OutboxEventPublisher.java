package com.wlstore.catalog_service.outbox;

import com.wlstore.catalog_service.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Publica UM evento da outbox no RabbitMQ e só retorna depois que o broker confirmou (ack).
 * Qualquer falha vira exceção: o evento continua pendente e será tentado de novo.
 */
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private static final long CONFIRM_TIMEOUT_SECONDS = 5;

    private final RabbitTemplate rabbitTemplate;

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
        rabbitTemplate.send(RabbitConfig.CATALOG_EVENTS_EXCHANGE, event.getEventType(), message, correlation);

        awaitConfirm(correlation, eventId);
    }

    private void awaitConfirm(CorrelationData correlation, String eventId) {
        try {
            CorrelationData.Confirm confirm = correlation.getFuture().get(CONFIRM_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!confirm.ack()) {
                throw new IllegalStateException("Broker nack for outbox event " + eventId + ": " + confirm.reason());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting broker confirm for outbox event " + eventId, e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("No broker confirm for outbox event " + eventId, e);
        }
    }
}