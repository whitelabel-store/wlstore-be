package com.wlstore.catalog_service.outbox;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayerTest {

    @Mock
    OutboxEventRepository repository;

    @Mock
    OutboxEventPublisher publisher;

    @InjectMocks
    OutboxRelayer relayer;

    @Test
    void relayPendingPublishesInOrderAndMarksEachAsPublished() {
        OutboxEvent first = event("product.created");
        OutboxEvent second = event("product.updated");
        when(repository.lockNextUnpublished(anyInt())).thenReturn(List.of(first, second));

        relayer.relayPending();

        InOrder order = inOrder(publisher);
        order.verify(publisher).publish(first);
        order.verify(publisher).publish(second);
        assertThat(first.getPublishedAt()).isNotNull();
        assertThat(second.getPublishedAt()).isNotNull();
    }

    @Test
    void relayPendingStopsAtFirstFailureToPreserveOrder() {
        OutboxEvent first = event("product.created");
        OutboxEvent second = event("product.updated");
        OutboxEvent third = event("product.deleted");
        when(repository.lockNextUnpublished(anyInt())).thenReturn(List.of(first, second, third));

        // cenário: o primeiro publica normalmente, o segundo falha
        doNothing().when(publisher).publish(first);
        doThrow(new IllegalStateException("broker down")).when(publisher).publish(second);

        relayer.relayPending();

        assertThat(first.getPublishedAt()).isNotNull();
        assertThat(second.getPublishedAt()).isNull();
        assertThat(third.getPublishedAt()).isNull();
        verify(publisher, never()).publish(third);
    }

    private OutboxEvent event(String eventType) {
        return OutboxEvent.of(UUID.randomUUID(), "PRODUCT", UUID.randomUUID(), eventType, "{\"status\":\"ACTIVE\"}");
    }
}