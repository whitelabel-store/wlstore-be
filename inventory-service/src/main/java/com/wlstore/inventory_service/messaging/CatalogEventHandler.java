package com.wlstore.inventory_service.messaging;

import com.wlstore.inventory_service.service.StockSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CatalogEventHandler {

    private final  ProcessedEventInbox inbox;
    private final StockSyncService stockSyncService;

    @Transactional
    public void handle(UUID messageId, Set<String> skus) {
        if (!inbox.registerIfFirstTime(messageId)) {
            return;
        }

        stockSyncService.ensureStockFor(skus);
    }
}
