package com.wlstore.pricing_service.outbox;

import com.wlstore.pricing_service.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class OutboxEventRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    OutboxEventRepository repository;

    @Autowired
    EntityManager entityManager;

    @Test
    void savesAndReadsBackJsonPayload() {
        UUID tenantId = UUID.randomUUID();
        UUID priceId = UUID.randomUUID();

        OutboxEvent saved = repository.saveAndFlush(OutboxEvent.of(
                tenantId,
                "PRICE",
                priceId,
                "price.changed",
                "{\"amountCents\":9000}"));

        entityManager.clear();

        OutboxEvent found = repository.findById(saved.getId()).orElseThrow();

        assertThat(found.getTenantId()).isEqualTo(tenantId);
        assertThat(found.getAggregateId()).isEqualTo(priceId);
        assertThat(found.getEventType()).isEqualTo("price.changed");
        assertThat(found.getPayload()).contains("9000");
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getPublishedAt()).isNull();
    }

    @Test
    void lockNextUnpublishedReturnsOnlyPendingEventsUpToLimit() {
        // Outros testes commitam eventos pendentes no mesmo banco. Esta limpeza vale só
        // dentro da transação do teste, que é desfeita no final.
        repository.deleteAll();
        repository.flush();

        OutboxEvent first = repository.save(event());
        OutboxEvent second = repository.save(event());
        OutboxEvent alreadyPublished = repository.save(event());
        alreadyPublished.markPublished();
        repository.flush();
        entityManager.clear();

        List<OutboxEvent> all = repository.lockNextUnpublished(10);
        assertThat(all)
                .extracting(OutboxEvent::getId)
                .containsExactlyInAnyOrder(first.getId(), second.getId());

        List<OutboxEvent> limited = repository.lockNextUnpublished(1);
        assertThat(limited).hasSize(1);
    }

    private OutboxEvent event() {
        return OutboxEvent.of(UUID.randomUUID(), "PRICE", UUID.randomUUID(), "price.changed", "{\"amountCents\":9000}");
    }
}