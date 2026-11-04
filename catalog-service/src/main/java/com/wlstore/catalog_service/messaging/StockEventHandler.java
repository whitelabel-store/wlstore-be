package com.wlstore.catalog_service.messaging;

import com.wlstore.catalog_service.model.VariantStock;
import com.wlstore.catalog_service.repository.VariantStockRepository;
import com.wlstore.common.messaging.ProcessedEventInbox;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StockEventHandler {

    private final ProcessedEventInbox inbox;
    private final VariantStockRepository variantStockRepository;

    @Transactional
    public void handle(UUID messageId, StockChangedMessage message) {
        if (!inbox.registerIfFirstTime(messageId)) {
            return;
        }

        Optional<VariantStock> existing = variantStockRepository.findBySku(message.sku());

        if (existing.isEmpty()) {
            variantStockRepository.save(VariantStock.builder()
                    .sku(message.sku())
                    .available(message.available())
                    .stockVersion(message.version())
                    .build());

            return;
        }

        VariantStock current = existing.get();

        if (message.version() <= current.getStockVersion()) {
            return;
        }

        current.setAvailable(message.available());
        current.setStockVersion(message.version());
    }
}
