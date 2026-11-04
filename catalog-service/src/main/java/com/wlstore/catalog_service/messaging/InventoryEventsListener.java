package com.wlstore.catalog_service.messaging;

import com.wlstore.catalog_service.config.RabbitConfig;
import com.wlstore.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InventoryEventsListener {
    private final ObjectMapper objectMapper;
    private final StockEventHandler handler;

    @RabbitListener(queues = RabbitConfig.INVENTORY_EVENTS_QUEUE)
    public void onMessage(Message message) {
        UUID messageId = UUID.fromString(message.getMessageProperties().getMessageId());

        StockChangedMessage stockChanged = objectMapper.readValue(message.getBody(), StockChangedMessage.class);

        TenantContext.runWithTenant(stockChanged.tenantId(), () -> handler.handle(messageId, stockChanged));
    }
}
