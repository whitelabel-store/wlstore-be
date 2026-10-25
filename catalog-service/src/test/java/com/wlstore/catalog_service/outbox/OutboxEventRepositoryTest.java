package com.wlstore.catalog_service.outbox;

import com.wlstore.catalog_service.AbstractIntegrationTest;
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
        UUID productId = UUID.randomUUID();

        OutboxEvent saved = repository.saveAndFlush(OutboxEvent.of(
                tenantId,
                "PRODUCT",
                productId,
                "product.created",
                "{\"status\":\"ACTIVE\"}"));

        // limpa o cache da sessão para forçar uma leitura real no banco
        entityManager.clear();

        OutboxEvent found = repository.findById(saved.getId()).orElseThrow();

        assertThat(found.getTenantId()).isEqualTo(tenantId);
        assertThat(found.getAggregateId()).isEqualTo(productId);
        assertThat(found.getEventType()).isEqualTo("product.created");
        assertThat(found.getPayload()).contains("ACTIVE");
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getPublishedAt()).isNull();
    }

    @Test
    void lockNextUnpublishedReturnsOnlyPendingEventsUpToLimit() {
        OutboxEvent first = repository.save(event("product.created"));
        OutboxEvent second = repository.save(event("product.updated"));
        OutboxEvent alreadyPublished = repository.save(event("product.deleted"));
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

    private OutboxEvent event(String eventType) {
        return OutboxEvent.of(UUID.randomUUID(), "PRODUCT", UUID.randomUUID(), eventType, "{\"status\":\"ACTIVE\"}");
    }
}