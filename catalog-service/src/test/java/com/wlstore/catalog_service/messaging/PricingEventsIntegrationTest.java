package com.wlstore.catalog_service.messaging;

import com.wlstore.catalog_service.AbstractIntegrationTest;
import com.wlstore.catalog_service.config.RabbitConfig;
import com.wlstore.catalog_service.model.VariantPrice;
import com.wlstore.catalog_service.repository.VariantPriceRepository;
import com.wlstore.common.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class PricingEventsIntegrationTest extends AbstractIntegrationTest {

    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    VariantPriceRepository variantPriceRepository;

    @Test
    void priceEventStoresThePriceForTheTenantInThePayload() {
        UUID tenant = UUID.randomUUID();
        UUID otherTenant = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        publish("price.changed", UUID.randomUUID(),
                priceJson(tenant, productId, "PRC-001", 9000L, 10000L, T0));

        awaitAmount(tenant, "PRC-001", 9000L);

        VariantPrice stored = priceOf(tenant, "PRC-001").orElseThrow();
        assertThat(stored.getProductId()).isEqualTo(productId);
        assertThat(stored.getOriginalAmountCents()).isEqualTo(10000L);
        assertThat(stored.getValidFrom()).isEqualTo(T0);
        assertThat(priceOf(otherTenant, "PRC-001")).isEmpty();
    }

    @Test
    void aNewerEventReplacesThePriceAndAnOlderOneIsIgnored() {
        UUID tenant = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        publish("price.changed", UUID.randomUUID(),
                priceJson(tenant, productId, "ORD-001", 10000L, null, T0));
        awaitAmount(tenant, "ORD-001", 10000L);

        publish("price.changed", UUID.randomUUID(),
                priceJson(tenant, productId, "ORD-001", 9000L, null, T0.plusSeconds(3600)));
        awaitAmount(tenant, "ORD-001", 9000L);

        // a late event: different message, but older than what is already stored
        publish("price.changed", UUID.randomUUID(),
                priceJson(tenant, productId, "ORD-001", 12345L, null, T0.minusSeconds(3600)));

        awaitMarker(tenant);

        assertThat(priceOf(tenant, "ORD-001").orElseThrow().getAmountCents()).isEqualTo(9000L);
    }

    @Test
    void redeliveredMessageIsIgnoredEvenWhenItLooksNewer() {
        UUID tenant = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID firstMessageId = UUID.randomUUID();

        publish("price.changed", firstMessageId,
                priceJson(tenant, productId, "DUP-001", 9000L, null, T0));
        awaitAmount(tenant, "DUP-001", 9000L);

        // same messageId, but different content and a NEWER validFrom: only the inbox can explain
        // why it is ignored (the ordering rule alone would accept it)
        publish("price.changed", firstMessageId,
                priceJson(tenant, productId, "DUP-001", 7000L, null, T0.plusSeconds(3600)));

        awaitMarker(tenant);

        assertThat(priceOf(tenant, "DUP-001").orElseThrow().getAmountCents()).isEqualTo(9000L);
    }

    @Test
    void poisonMessageGoesToTheDeadLetterQueueAndDoesNotBlockTheNext() {
        publish("price.changed", UUID.randomUUID(), "{ this is not json");

        Message dead = rabbitTemplate.receive(RabbitConfig.PRICING_EVENTS_DLQ, 15_000);
        assertThat(dead).isNotNull();

        UUID tenant = UUID.randomUUID();
        publish("price.changed", UUID.randomUUID(),
                priceJson(tenant, UUID.randomUUID(), "POISON-OK1", 5000L, null, T0));
        awaitAmount(tenant, "POISON-OK1", 5000L);
    }

    /** The queue is read in order by one consumer: once the marker is stored, what came before was handled. */
    private void awaitMarker(UUID tenant) {
        String markerSku = "MARK-" + UUID.randomUUID().toString().substring(0, 8);
        publish("price.changed", UUID.randomUUID(),
                priceJson(tenant, UUID.randomUUID(), markerSku, 1L, null, T0));
        awaitAmount(tenant, markerSku, 1L);
    }

    private void publish(String routingKey, UUID messageId, String json) {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setMessageId(messageId.toString());
        rabbitTemplate.send(RabbitConfig.PRICING_EVENTS_EXCHANGE, routingKey,
                new Message(json.getBytes(StandardCharsets.UTF_8), properties));
    }

    private String priceJson(UUID tenant, UUID productId, String sku, long amountCents,
                             Long originalAmountCents, Instant validFrom) {
        return ("{\"tenantId\":\"%s\",\"productId\":\"%s\",\"sku\":\"%s\",\"amountCents\":%d,"
                + "\"originalAmountCents\":%s,\"validFrom\":\"%s\"}")
                .formatted(tenant, productId, sku, amountCents, originalAmountCents, validFrom);
    }

    private Optional<VariantPrice> priceOf(UUID tenant, String sku) {
        return TenantContext.callWithTenant(tenant, () -> variantPriceRepository.findBySku(sku));
    }

    private void awaitAmount(UUID tenant, String sku, long expected) {
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(priceOf(tenant, sku))
                        .isPresent()
                        .get()
                        .extracting(VariantPrice::getAmountCents)
                        .isEqualTo(expected));
    }
}