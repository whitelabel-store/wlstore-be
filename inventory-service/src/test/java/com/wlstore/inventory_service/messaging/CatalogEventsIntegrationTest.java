package com.wlstore.inventory_service.messaging;

import com.wlstore.common.tenant.TenantContext;
import com.wlstore.inventory_service.AbstractIntegrationTest;
import com.wlstore.inventory_service.config.RabbitConfig;
import com.wlstore.inventory_service.model.Stock;
import com.wlstore.inventory_service.repository.StockRepository;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class CatalogEventsIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    StockRepository stockRepository;

    @Test
    void productEventCreatesEmptyStockForEachSkuInTheTenantOfThePayload() {
        UUID tenant = UUID.randomUUID();
        UUID otherTenant = UUID.randomUUID();

        publish("product.created", UUID.randomUUID(), snapshot(UUID.randomUUID(), tenant, "INV-A1", "INV-A2"));

        awaitSkus(tenant, "INV-A1", "INV-A2");
        assertThat(stockOf(tenant, "INV-A1").getQuantityOnHand()).isZero();
        assertThat(stockOf(tenant, "INV-A1").getQuantityReserved()).isZero();
        assertThat(skusOf(otherTenant)).isEmpty();
    }

    @Test
    void anUpdateAddsNewSkusButNeverTouchesTheCountsOfExistingOnes() {
        UUID tenant = UUID.randomUUID();
        givenStock(tenant, "KEEP-A", 10, 2);

        publish("product.updated", UUID.randomUUID(), snapshot(UUID.randomUUID(), tenant, "KEEP-A", "KEEP-B"));

        awaitSkus(tenant, "KEEP-A", "KEEP-B");
        assertThat(stockOf(tenant, "KEEP-A").getQuantityOnHand()).isEqualTo(10);
        assertThat(stockOf(tenant, "KEEP-A").getQuantityReserved()).isEqualTo(2);
        assertThat(stockOf(tenant, "KEEP-B").getQuantityOnHand()).isZero();
    }

    @Test
    void skusRemovedFromTheCatalogOrProductsDeletedKeepTheirStock() {
        UUID tenant = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        publish("product.created", UUID.randomUUID(), snapshot(productId, tenant, "GONE-A", "GONE-B"));
        awaitSkus(tenant, "GONE-A", "GONE-B");
        setOnHand(tenant, "GONE-B", 7);

        publish("product.updated", UUID.randomUUID(), snapshot(productId, tenant, "GONE-A"));
        publish("product.deleted", UUID.randomUUID(), snapshot(productId, tenant, "GONE-A"));
        awaitMarker(tenant);

        assertThat(skusOf(tenant)).contains("GONE-A", "GONE-B");
        assertThat(stockOf(tenant, "GONE-B").getQuantityOnHand()).isEqualTo(7);
    }

    @Test
    void redeliveredMessageIsIgnored() {
        UUID tenant = UUID.randomUUID();
        UUID firstMessageId = UUID.randomUUID();

        publish("product.created", firstMessageId, snapshot(UUID.randomUUID(), tenant, "DUP-A"));
        awaitSkus(tenant, "DUP-A");

        // same messageId, different content: only the inbox can explain why it is ignored
        publish("product.created", firstMessageId, snapshot(UUID.randomUUID(), tenant, "DUP-B"));
        awaitMarker(tenant);

        assertThat(skusOf(tenant)).contains("DUP-A").doesNotContain("DUP-B");
    }

    @Test
    void poisonMessageGoesToTheDeadLetterQueueAndDoesNotBlockTheNext() {
        publish("product.created", UUID.randomUUID(), "{ this is not json");

        Message dead = rabbitTemplate.receive(RabbitConfig.CATALOG_EVENTS_DLQ, 15_000);
        assertThat(dead).isNotNull();

        UUID tenant = UUID.randomUUID();
        publish("product.created", UUID.randomUUID(), snapshot(UUID.randomUUID(), tenant, "POISON-OK1"));
        awaitSkus(tenant, "POISON-OK1");
    }

    /** One consumer reads the queue in order: once the marker exists, everything published before it was handled. */
    private void awaitMarker(UUID tenant) {
        String markerSku = "MARK-" + UUID.randomUUID().toString().substring(0, 8);
        publish("product.created", UUID.randomUUID(), snapshot(UUID.randomUUID(), tenant, markerSku));
        awaitSkus(tenant, markerSku);
    }

    private void publish(String routingKey, UUID messageId, String json) {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setMessageId(messageId.toString());
        rabbitTemplate.send(RabbitConfig.CATALOG_EXCHANGE, routingKey,
                new Message(json.getBytes(StandardCharsets.UTF_8), properties));
    }

    private String snapshot(UUID productId, UUID tenant, String... skus) {
        String variants = Arrays.stream(skus)
                .map(sku -> "{\"sku\":\"%s\",\"status\":\"ACTIVE\"}".formatted(sku))
                .collect(Collectors.joining(","));
        return "{\"productId\":\"%s\",\"tenantId\":\"%s\",\"status\":\"ACTIVE\",\"variants\":[%s]}"
                .formatted(productId, tenant, variants);
    }

    private List<String> skusOf(UUID tenant) {
        return TenantContext.callWithTenant(tenant, () -> stockRepository.findAll())
                .stream()
                .map(Stock::getSku)
                .sorted()
                .toList();
    }

    private Stock stockOf(UUID tenant, String sku) {
        return TenantContext.callWithTenant(tenant, () -> stockRepository.findBySku(sku).orElseThrow());
    }

    private void givenStock(UUID tenant, String sku, int onHand, int reserved) {
        TenantContext.runWithTenant(tenant, () -> stockRepository.saveAndFlush(
                Stock.builder().sku(sku).quantityOnHand(onHand).quantityReserved(reserved).build()));
    }

    private void setOnHand(UUID tenant, String sku, int onHand) {
        TenantContext.runWithTenant(tenant, () -> {
            Stock stock = stockRepository.findBySku(sku).orElseThrow();
            stock.setQuantityOnHand(onHand);
            stockRepository.saveAndFlush(stock);
        });
    }

    private void awaitSkus(UUID tenant, String... expected) {
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(skusOf(tenant)).contains(expected));
    }
}