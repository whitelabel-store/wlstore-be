package com.wlstore.inventory_service.outbox;

import com.wlstore.common.exception.ResourceNotFoundException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfirmOutboxIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    StockService stockService;

    @Autowired
    StockRepository stockRepository;

    @Autowired
    OutboxEventRepository outboxEventRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void confirmingRecordsAStockChangedEventPerLineWithTheConsumedBalance() {
        UUID tenant = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        givenStock(tenant, "CNF-A", 10);
        givenStock(tenant, "CNF-B", 5);

        reserve(tenant, orderId, line("CNF-A", 3), line("CNF-B", 2));
        assertThat(eventsOf(tenant)).hasSize(2);        // the two reservations

        confirm(tenant, orderId);

        assertThat(eventsOf(tenant)).hasSize(4);        // plus one per confirmed line

        List<StockChangedMessage> forA = messagesOf(tenant, "CNF-A");
        assertThat(forA).hasSize(2);
        assertThat(forA.get(0).version()).isLessThan(forA.get(1).version());
        StockChangedMessage lastA = forA.get(1);
        assertThat(lastA.quantityOnHand()).isEqualTo(7);
        assertThat(lastA.quantityReserved()).isZero();
        assertThat(lastA.available()).isEqualTo(7);

        StockChangedMessage lastB = lastMessageOf(tenant, "CNF-B");
        assertThat(lastB.quantityOnHand()).isEqualTo(3);
        assertThat(lastB.quantityReserved()).isZero();
        assertThat(lastB.available()).isEqualTo(3);
    }

    @Test
    void confirmingTheSameOrderTwiceRecordsTheEventsOnlyOnce() {
        UUID tenant = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        givenStock(tenant, "CNF-TWICE", 10);

        reserve(tenant, orderId, line("CNF-TWICE", 3));
        confirm(tenant, orderId);
        confirm(tenant, orderId);   // the retry after a lost response

        // one for the reservation, one for the confirmation; the repeat added nothing
        assertThat(eventsOf(tenant)).hasSize(2);
    }

    @Test
    void confirmingAnUnknownOrderRecordsNoEvent() {
        UUID tenant = UUID.randomUUID();
        givenStock(tenant, "CNF-NONE", 10);

        // First a real change, so "still one event" proves the confirmation added nothing
        // (without it the test would also pass while the feature does not exist yet).
        reserve(tenant, UUID.randomUUID(), line("CNF-NONE", 1));
        assertThat(eventsOf(tenant)).hasSize(1);

        assertThatThrownBy(() -> confirm(tenant, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThat(eventsOf(tenant)).hasSize(1);
    }

    // ---------- helpers ----------

    private ReservationLineDTO line(String sku, int quantity) {
        return new ReservationLineDTO(sku, quantity);
    }

    private void reserve(UUID tenant, UUID orderId, ReservationLineDTO... lines) {
        TenantContext.runWithTenant(tenant, () -> stockService.reserveAll(orderId, List.of(lines)));
    }

    private void confirm(UUID tenant, UUID orderId) {
        TenantContext.runWithTenant(tenant, () -> stockService.confirm(orderId));
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