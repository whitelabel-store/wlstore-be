package com.wlstore.catalog_service.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Lê os eventos pendentes da outbox, publica no RabbitMQ e marca como publicados.
 * Entrega at-least-once: se o processo cair depois de publicar e antes do commit,
 * o evento é publicado de novo (os consumidores deduplicam pelo messageId).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "wlstore.outbox.relay", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelayer {

    private static final int BATCH_SIZE = 50;

    private final OutboxEventRepository repository;
    private final OutboxEventPublisher publisher;

    @Scheduled(fixedDelayString = "${wlstore.outbox.relay.interval-ms:1000}")
    @Transactional
    public void relayPending() {
        List<OutboxEvent> pending = repository.lockNextUnpublished(BATCH_SIZE);

        for (OutboxEvent event : pending) {
            try {
                publisher.publish(event);
            } catch (RuntimeException ex) {
                // Para aqui: publicar os seguintes tiraria os eventos de ordem.
                log.warn("Failed to publish outbox event {} ({}); will retry on next cycle",
                        event.getId(), event.getEventType(), ex);
                return;
            }
            event.markPublished();
        }
    }
}