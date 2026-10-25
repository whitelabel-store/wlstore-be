package com.wlstore.pricing_service.messaging;

import com.wlstore.common.tenant.TenantContext;
import com.wlstore.pricing_service.AbstractIntegrationTest;
import com.wlstore.pricing_service.config.RabbitConfig;
import com.wlstore.pricing_service.model.KnownSku;
import com.wlstore.pricing_service.repository.KnownSkuRepository;
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

    private static final String EXCHANGE = "catalog.events";

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    KnownSkuRepository knownSkuRepository;

    @Test
    void productEventRegistersItsSkusForTheTenantInThePayload() {
        UUID tenant = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        publish("product.created", UUID.randomUUID(),
                snapshot(productId, tenant, "MSG-A1", "MSG-A2"));

        awaitSkus(tenant, productId, "MSG-A1", "MSG-A2");
    }

    @Test
    void redeliveredMessageIsIgnored() {
        UUID tenant = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID firstMessageId = UUID.randomUUID();

        publish("product.created", firstMessageId,
                snapshot(productId, tenant, "DUP-A1", "DUP-A2"));
        awaitSkus(tenant, productId, "DUP-A1", "DUP-A2");

        publish("product.updated", UUID.randomUUID(),
                snapshot(productId, tenant, "DUP-A1"));
        awaitSkus(tenant, productId, "DUP-A1");

        // the broker delivers the first message again (same messageId)
        publish("product.created", firstMessageId,
                snapshot(productId, tenant, "DUP-A1", "DUP-A2"));

        // marker: once it is processed, the redelivery sent before it was handled too
        UUID markerProduct = UUID.randomUUID();
        publish("product.created", UUID.randomUUID(),
                snapshot(markerProduct, tenant, "DUP-MARK"));
        awaitSkus(tenant, markerProduct, "DUP-MARK");

        assertThat(skusOf(tenant, productId)).containsExactly("DUP-A1");
    }

    @Test
    void poisonMessageGoesToTheDeadLetterQueueAndDoesNotBlockTheNext() {
        publish("product.created", UUID.randomUUID(), "{ this is not json");

        Message dead = rabbitTemplate.receive(RabbitConfig.CATALOG_EVENTS_DLQ, 15_000);
        assertThat(dead).isNotNull();

        UUID tenant = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        publish("product.created", UUID.randomUUID(),
                snapshot(productId, tenant, "POISON-OK1"));
        awaitSkus(tenant, productId, "POISON-OK1");
    }

    private void publish(String routingKey, UUID messageId, String json) {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setMessageId(messageId.toString());
        rabbitTemplate.send(EXCHANGE, routingKey,
                new Message(json.getBytes(StandardCharsets.UTF_8), properties));
    }

    private String snapshot(UUID productId, UUID tenant, String... skus) {
        String variants = Arrays.stream(skus)
                .map(sku -> "{\"sku\":\"%s\",\"status\":\"ACTIVE\"}".formatted(sku))
                .collect(Collectors.joining(","));
        return "{\"productId\":\"%s\",\"tenantId\":\"%s\",\"status\":\"ACTIVE\",\"variants\":[%s]}"
                .formatted(productId, tenant, variants);
    }

    private List<String> skusOf(UUID tenant, UUID productId) {
        return TenantContext.callWithTenant(tenant,
                        () -> knownSkuRepository.findByProductId(productId))
                .stream()
                .map(KnownSku::getSku)
                .sorted()
                .toList();
    }

    private void awaitSkus(UUID tenant, UUID productId, String... expected) {
        List<String> sorted = Arrays.stream(expected).sorted().toList();
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(skusOf(tenant, productId)).isEqualTo(sorted));
    }
}