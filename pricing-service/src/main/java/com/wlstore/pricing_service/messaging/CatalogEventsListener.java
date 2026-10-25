package com.wlstore.pricing_service.messaging;

import com.wlstore.common.tenant.TenantContext;
import com.wlstore.pricing_service.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CatalogEventsListener {
    private static final String PRODUCT_DELETED = "product.deleted";

    private final ObjectMapper objectMapper;
    private final CatalogEventHandler handler;

    @RabbitListener(queues = RabbitConfig.CATALOG_EVENTS_QUEUE)
    public void onMessage(Message message) {
        UUID messageId = UUID.fromString(message.getMessageProperties().getMessageId());
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();

        ProductSnapshotMessage snapshot = objectMapper.readValue(message.getBody(), ProductSnapshotMessage.class);

        Set<String> skus = PRODUCT_DELETED.equals(routingKey) ? Set.of() : snapshot.skus();

        TenantContext.runWithTenant(snapshot.tenantId(), () -> handler.handle(messageId, snapshot.productId(), skus));
    }
}
