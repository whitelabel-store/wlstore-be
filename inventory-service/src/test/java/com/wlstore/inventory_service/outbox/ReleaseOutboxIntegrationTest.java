package com.wlstore.inventory_service.outbox;

import com.wlstore.common.outbox.OutboxEvent;
import com.wlstore.common.outbox.OutboxEventRepository;
import com.wlstore.common.tenant.TenantContext;
import com.wlstore.inventory_service.AbstractIntegrationTest;
import com.wlstore.inventory_service.dto.ReservationLineDTO;
import com.wlstore.inventory_service.model.Stock;
import com.wlstore.inventory_service.repository.StockRepository;
import com.wlstore.inventory_service.service.StockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReleaseOutboxIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    StockService stockService;

    @Autowired
    StockRepository stockRepository;

    @Autowired
    OutboxEventRepository outboxEventRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void releasingRecordsAStockChangedEventPerLineWithTheBalanceGivenBack() {
        UUID tenant = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        givenStock(tenant, "REL-A", 10);
        givenStock(tenant, "REL-B", 5);

        reserve(tenant, orderId, line("REL-A", 3), line("REL-B", 2));
        assertThat(eventsOf(tenant)).hasSize(2);        // the two reservations

        release(tenant, orderId);

        assertThat(eventsOf(tenant)).hasSize(4);        // plus one per released line

        List<StockChangedMessage> forA = messagesOf(tenant, "REL-A");
        assertThat(forA).hasSize(2);
        assertThat(forA.get(0).version()).isLessThan(forA.get(1).version());
        StockChangedMessage lastA = forA.get(1);
        assertThat(lastA.quantityOnHand()).isEqualTo(10);
        assertThat(lastA.quantityReserved()).isZero();
        assertThat(lastA.available()).isEqualTo(10);

        StockChangedMessage lastB = lastMessageOf(tenant, "REL-B");
        assertThat(lastB.quantityReserved()).isZero();
        assertThat(lastB.available()).isEqualTo(5);
    }

    @Test
    void releasingTheSameOrderTwiceRecordsTheEventsOnlyOnce() {
        UUID tenant = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        givenStock(tenant, "REL-TWICE", 10);

        reserve(tenant, orderId, line("REL-TWICE", 3));
        release(tenant, orderId);
        release(tenant, orderId);   // the retry after a lost response

        // one for the reservation, one for the release; the repeat added nothing
        assertThat(eventsOf(tenant)).hasSize(2);
    }

    @Test
    void releasingAnUnknownOrderRecordsNoEvent() {
        UUID tenant = UUID.randomUUID();
        givenStock(tenant, "REL-NONE", 10);

        // First a real change, so "still one event" proves the release added nothing
        // (without it the test would also pass while the feature does not exist yet).
        reserve(tenant, UUID.randomUUID(), line("REL-NONE", 1));
        assertThat(eventsOf(tenant)).hasSize(1);

        release(tenant, UUID.randomUUID());   // never reserved anything

        assertThat(eventsOf(tenant)).hasSize(1);
    }

    // ---------- helpers ----------

    private ReservationLineDTO line(String sku, int quantity) {
        return new ReservationLineDTO(sku, quantity);
    }

    private void reserve(UUID tenant, UUID orderId, ReservationLineDTO... lines) {
        TenantContext.runWithTenant(tenant, () -> stockService.reserveAll(orderId, List.of(lines)));
    }

    private void release(UUID tenant, UUID orderId) {
        TenantContext.runWithTenant(tenant, () -> stockService.release(orderId));
    }

    private List<OutboxEvent> eventsOf(UUID tenant) {
        return outboxEventRepository.findAll().stream()
                .filter(event -> event.getTenantId().equals(tenant))
                .sorted(Comparator.comparing(OutboxEvent::getCreatedAt))
                .toList();
    }

    private List<StockChangedMessage> messagesOf(UUID tenant, String sku) {
        return eventsOf(tenant).stream()
                .map(event -> objectMapper.readValue(event.getPayload(), StockChangedMessage.class))
                .filter(message -> message.sku().equals(sku))
                .sorted(Comparator.comparingLong(StockChangedMessage::version))
                .toList();
    }

    private StockChangedMessage lastMessageOf(UUID tenant, String sku) {
        List<StockChangedMessage> messages = messagesOf(tenant, sku);
        return messages.get(messages.size() - 1);
    }

    private void givenStock(UUID tenant, String sku, int onHand) {
        TenantContext.runWithTenant(tenant, () -> stockRepository.saveAndFlush(
                Stock.builder().sku(sku).quantityOnHand(onHand).quantityReserved(0).build()));
    }
}