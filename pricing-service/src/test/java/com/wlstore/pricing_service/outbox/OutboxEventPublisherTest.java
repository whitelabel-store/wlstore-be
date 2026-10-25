package com.wlstore.pricing_service.outbox;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherTest {

    @Mock
    RabbitTemplate rabbitTemplate;

    @InjectMocks
    OutboxEventPublisher publisher;

    @Test
    void publishSendsPayloadToExchangeUsingEventTypeAsRoutingKey() {
        UUID eventId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        OutboxEvent event = eventWith(eventId, tenantId, "{\"amountCents\":9000}");
        brokerAnswers(true);

        publisher.publish(event);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(
                eq("pricing.events"), eq("price.changed"), captor.capture(), any(CorrelationData.class));

        Message sent = captor.getValue();
        assertThat(new String(sent.getBody(), StandardCharsets.UTF_8)).isEqualTo("{\"amountCents\":9000}");
        assertThat(sent.getMessageProperties().getContentType()).isEqualTo("application/json");
        assertThat(sent.getMessageProperties().getMessageId()).isEqualTo(eventId.toString());
        assertThat(sent.getMessageProperties().getHeaders()).containsEntry("tenantId", tenantId.toString());
    }

    @Test
    void publishFailsWhenBrokerNacks() {
        OutboxEvent event = eventWith(UUID.randomUUID(), UUID.randomUUID(), "{\"amountCents\":9000}");
        brokerAnswers(false);

        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("nack");
    }

    /** Simula o broker: completa o future de confirmação com ack ou nack. */
    private void brokerAnswers(boolean ack) {
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(ack, ack ? null : "nack"));
            return null;
        }).when(rabbitTemplate).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
    }

    private OutboxEvent eventWith(UUID id, UUID tenantId, String payload) {
        OutboxEvent event = OutboxEvent.of(tenantId, "PRICE", UUID.randomUUID(), "price.changed", payload);
        // o id só é gerado ao persistir; no teste unitário não há banco, então ajustamos na mão
        ReflectionTestUtils.setField(event, "id", id);
        return event;
    }
}