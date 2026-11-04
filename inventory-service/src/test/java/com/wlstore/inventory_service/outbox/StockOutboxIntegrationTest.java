package com.wlstore.inventory_service.outbox;

import com.wlstore.common.outbox.OutboxEvent;
import com.wlstore.common.outbox.OutboxEventRepository;
import com.wlstore.common.tenant.TenantContext;
import com.wlstore.inventory_service.AbstractIntegrationTest;
import com.wlstore.inventory_service.exception.InsufficientStockException;
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

class StockOutboxIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    StockService stockService;

    @Autowired
    StockRepository stockRepository;

    @Autowired
    OutboxEventRepository outboxEventRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void reservingRecordsAStockChangedEventWithTheNewBalance() {
        UUID tenant = UUID.randomUUID();
        givenStock(tenant, "OBX-RES", 10, 0);

        TenantContext.runWithTenant(tenant, () -> stockService.reserve("OBX-RES", 3));

        Stock stock = stockOf(tenant, "OBX-RES");
        List<OutboxEvent> events = eventsOf(tenant);

        assertThat(events).hasSize(1);
        OutboxEvent event = events.get(0);
        assertThat(event.getEventType()).isEqualTo("stock.changed");
        assertThat(event.getAggregateType()).isEqualTo("STOCK");
        assertThat(event.getAggregateId()).isEqualTo(stock.getId());
        assertThat(event.getPublishedAt()).isNull();

        StockChangedMessage message = read(event);
        assertThat(message.tenantId()).isEqualTo(tenant);
        assertThat(message.sku()).isEqualTo("OBX-RES");
        assertThat(message.quantityOnHand()).isEqualTo(10);
        assertThat(message.quantityReserved()).isEqualTo(3);
        assertThat(message.available()).isEqualTo(7);
        assertThat(message.version()).isEqualTo(stock.getVersion());
    }

    @Test
    void settingOnHandRecordsAStockChangedEvent() {
        UUID tenant = UUID.randomUUID();
        givenStock(tenant, "OBX-SET", 5, 3);

        TenantContext.runWithTenant(tenant, () -> stockService.setOnHand("OBX-SET", 20));

        List<OutboxEvent> events = eventsOf(tenant);

        assertThat(events).hasSize(1);
        StockChangedMessage message = read(events.get(0));
        assertThat(message.sku()).isEqualTo("OBX-SET");
        assertThat(message.quantityOnHand()).isEqualTo(20);
        assertThat(message.quantityReserved()).isEqualTo(3);
        assertThat(message.available()).isEqualTo(17);
    }

    @Test
    void aRefusedReservationRecordsNoNewEvent() {
        UUID tenant = UUID.randomUUID();
        givenStock(tenant, "OBX-REF", 5, 3);

        // First a real change, so "still one event" proves the refusal added nothing
        // (without it the test would also pass while the feature does not exist yet).
        TenantContext.runWithTenant(tenant, () -> stockService.reserve("OBX-REF", 1));
        assertThat(eventsOf(tenant)).hasSize(1);

        assertThatThrownBy(() -> TenantContext.runWithTenant(tenant, () -> stockService.reserve("OBX-REF", 5)))
                .isInstanceOf(InsufficientStockException.class);

        assertThat(eventsOf(tenant)).hasSize(1);
    }

    @Test
    void eachChangeCarriesAHigherVersionThanThePreviousOne() {
        UUID tenant = UUID.randomUUID();
        givenStock(tenant, "OBX-VER", 10, 0);

        TenantContext.runWithTenant(tenant, () -> stockService.reserve("OBX-VER", 1));
        TenantContext.runWithTenant(tenant, () -> stockService.reserve("OBX-VER", 1));
        TenantContext.runWithTenant(tenant, () -> stockService.setOnHand("OBX-VER", 50));

        List<StockChangedMessage> messages = eventsOf(tenant).stream().map(this::read).toList();

        assertThat(messages).hasSize(3);
        assertThat(messages.get(0).version()).isLessThan(messages.get(1).version());
        assertThat(messages.get(1).version()).isLessThan(messages.get(2).version());
        assertThat(messages.get(2).quantityOnHand()).isEqualTo(50);
        assertThat(messages.get(2).quantityReserved()).isEqualTo(2);
    }

    /** The outbox has no tenant filter on purpose (the relayer sees all tenants), so filter here. */
    private List<OutboxEvent> eventsOf(UUID tenant) {
        return outboxEventRepository.findAll().stream()
                .filter(event -> event.getTenantId().equals(tenant))
                .sorted(Comparator.comparing(OutboxEvent::getCreatedAt))
                .toList();
    }

    private StockChangedMessage read(OutboxEvent event) {
        return objectMapper.readValue(event.getPayload(), StockChangedMessage.class);
    }

    private void givenStock(UUID tenant, String sku, int onHand, int reserved) {
        TenantContext.runWithTenant(tenant, () -> stockRepository.saveAndFlush(
                Stock.builder().sku(sku).quantityOnHand(onHand).quantityReserved(reserved).build()));
    }

    private Stock stockOf(UUID tenant, String sku) {
        return TenantContext.callWithTenant(tenant, () -> stockRepository.findBySku(sku).orElseThrow());
    }
}