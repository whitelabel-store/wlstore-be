package com.wlstore.pricing_service.messaging;

import com.wlstore.pricing_service.service.SkuSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CatalogEventHandler {

    private final ProcessedEventInbox inbox;
    private final SkuSyncService skuSyncService;

    @Transactional
    public void handle(UUID messageId, UUID productId, Set<String> skus) {
        if (!inbox.registerIfFirstTime(messageId)) {
            return;
        }

        skuSyncService.sync(productId, skus);
    }
}
