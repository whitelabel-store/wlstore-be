package com.wlstore.catalog_service.messaging;

import com.wlstore.catalog_service.AbstractIntegrationTest;
import com.wlstore.catalog_service.config.RabbitConfig;
import com.wlstore.catalog_service.model.VariantStock;
import com.wlstore.catalog_service.repository.VariantStockRepository;
import com.wlstore.common.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class StockEventsIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    VariantStockRepository variantStockRepository;

    @Test
    void stockEventStoresTheBalanceForTheTenantInThePayload() {
        UUID tenant = UUID.randomUUID();
        UUID otherTenant = UUID.randomUUID();

        publish("stock.changed", UUID.randomUUID(), stockJson(tenant, "STK-001", 20, 5, 3));

        awaitAvailable(tenant, "STK-001", 15);

        VariantStock stored = stockOf(tenant, "STK-001").orElseThrow();
        assertThat(stored.getStockVersion()).isEqualTo(3L);
        assertThat(stockOf(otherTenant, "STK-001")).isEmpty();
    }

    @Test
    void aNewerVersionReplacesTheBalanceAndAnOlderOneIsIgnored() {
        UUID tenant = UUID.randomUUID();

        publish("stock.changed", UUID.randomUUID(), stockJson(tenant, "ORD-STK", 10, 0, 1));
        awaitAvailable(tenant, "ORD-STK", 10);

        publish("stock.changed", UUID.randomUUID(), stockJson(tenant, "ORD-STK", 10, 3, 2));
        awaitAvailable(tenant, "ORD-STK", 7);

        // a late event: a different message, but with a version older than the one already stored
        publish("stock.changed", UUID.randomUUID(), stockJson(tenant, "ORD-STK", 99, 0, 1));

        awaitMarker(tenant);

        assertThat(stockOf(tenant, "ORD-STK").orElseThrow().getAvailable()).isEqualTo(7);
    }

    @Test
    void redeliveredMessageIsIgnoredEvenWhenItLooksNewer() {
        UUID tenant = UUID.randomUUID();
        UUID firstMessageId = UUID.randomUUID();

        publish("stock.changed", firstMessageId, stockJson(tenant, "DUP-STK", 10, 0, 1));
        awaitAvailable(tenant, "DUP-STK", 10);

        // same messageId, different content and a NEWER version: only the inbox can explain
        // why it is ignored (the version rule alone would accept it)
        publish("stock.changed", firstMessageId, stockJson(tenant, "DUP-STK", 10, 7, 2));

        awaitMarker(tenant);

        assertThat(stockOf(tenant, "DUP-STK").orElseThrow().getAvailable()).isEqualTo(10);
    }

    @Test
    void poisonMessageGoesToTheDeadLetterQueueAndDoesNotBlockTheNext() {
        publish("stock.changed", UUID.randomUUID(), "{ this is not json");

        Message dead = rabbitTemplate.receive(RabbitConfig.INVENTORY_EVENTS_DLQ, 15_000);
        assertThat(dead).isNotNull();

        UUID tenant = UUID.randomUUID();
        publish("stock.changed", UUID.randomUUID(), stockJson(tenant, "POISON-STK", 4, 0, 1));
        awaitAvailable(tenant, "POISON-STK", 4);
    }

    /** The queue is read in order by one consumer: once the marker is stored, what came before was handled. */
    private void awaitMarker(UUID tenant) {
        String markerSku = "MARK-" + UUID.randomUUID().toString().substring(0, 8);
        publish("stock.changed", UUID.randomUUID(), stockJson(tenant, markerSku, 1, 0, 1));
        awaitAvailable(tenant, markerSku, 1);
    }

    private void publish(String routingKey, UUID messageId, String json) {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setMessageId(messageId.toString());
        rabbitTemplate.send(RabbitConfig.INVENTORY_EVENTS_EXCHANGE, routingKey,
                new Message(json.getBytes(StandardCharsets.UTF_8), properties));
    }

    private String stockJson(UUID tenant, String sku, int onHand, int reserved, long version) {
        return ("{\"tenantId\":\"%s\",\"sku\":\"%s\",\"quantityOnHand\":%d,\"quantityReserved\":%d,"
                + "\"available\":%d,\"version\":%d}")
                .formatted(tenant, sku, onHand, reserved, onHand - reserved, version);
    }

    private Optional<VariantStock> stockOf(UUID tenant, String sku) {
        return TenantContext.callWithTenant(tenant, () -> variantStockRepository.findBySku(sku));
    }

    private void awaitAvailable(UUID tenant, String sku, int expected) {
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(stockOf(tenant, sku))
                        .isPresent()
                        .get()
                        .extracting(VariantStock::getAvailable)
                        .isEqualTo(expected));
    }
}