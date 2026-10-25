package com.wlstore.pricing_service.outbox;

import com.wlstore.pricing_service.AbstractIntegrationTest;
import com.wlstore.pricing_service.model.Price;
import com.wlstore.pricing_service.service.PriceService;
import com.wlstore.pricing_service.service.SkuSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PriceOutboxIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    PriceService priceService;

    @Autowired
    SkuSyncService skuSyncService;

    @Autowired
    OutboxEventRepository outboxRepository;

    @Test
    void definingAPriceRecordsAPriceChangedEvent() {
        UUID productId = UUID.randomUUID();
        skuSyncService.sync(productId, Set.of("OBX-001"));

        Price price = priceService.definePrice("OBX-001", 9000L, 10000L);

        List<OutboxEvent> events = eventsOf(price);

        assertThat(events).hasSize(1);
        OutboxEvent event = events.get(0);
        assertThat(event.getEventType()).isEqualTo("price.changed");
        assertThat(event.getAggregateType()).isEqualTo("PRICE");
        assertThat(event.getTenantId()).isEqualTo(price.getTenantId());
        assertThat(event.getPublishedAt()).isNull();
        assertThat(event.getPayload())
                .contains("OBX-001")
                .contains("9000")
                .contains("10000")
                .contains(productId.toString());
    }

    @Test
    void definingTheSamePriceAgainRecordsNoNewEvent() {
        skuSyncService.sync(UUID.randomUUID(), Set.of("OBX-002"));

        Price first = priceService.definePrice("OBX-002", 9000L, 10000L);
        Price second = priceService.definePrice("OBX-002", 9000L, 10000L);

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(eventsOf(first)).hasSize(1);
    }

    @Test
    void eachRealChangeRecordsItsOwnEvent() {
        skuSyncService.sync(UUID.randomUUID(), Set.of("OBX-003"));

        Price first = priceService.definePrice("OBX-003", 10000L, null);
        Price second = priceService.definePrice("OBX-003", 9000L, 10000L);

        assertThat(second.getId()).isNotEqualTo(first.getId());
        assertThat(eventsOf(first)).hasSize(1);
        assertThat(eventsOf(second)).hasSize(1);
        assertThat(eventsOf(second).get(0).getPayload()).contains("9000");
    }

    private List<OutboxEvent> eventsOf(Price price) {
        return outboxRepository.findAll().stream()
                .filter(event -> event.getAggregateId().equals(price.getId()))
                .toList();
    }
}