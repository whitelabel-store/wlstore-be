package com.wlstore.catalog_service.messaging;

import com.wlstore.catalog_service.model.VariantPrice;
import com.wlstore.catalog_service.repository.VariantPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PriceEventHandler {

    private final ProcessedEventInbox inbox;
    private final VariantPriceRepository variantPriceRepository;

    @Transactional
    public void handle(UUID messageId, PriceChangedMessage message) {
        if (!inbox.registerIfFirstTime(messageId)) {
            return;
        }

        Optional<VariantPrice> existing = variantPriceRepository.findBySku(message.sku());

        if (existing.isEmpty()) {
            variantPriceRepository.save(VariantPrice.builder()
                    .productId(message.productId())
                    .sku(message.sku())
                    .amountCents(message.amountCents())
                    .originalAmountCents(message.originalAmountCents())
                    .validFrom(message.validFrom())
                    .build());
            return;
        }

        VariantPrice current = existing.get();

        if (!message.validFrom().isAfter(current.getValidFrom())) {
            return;
        }

        current.setProductId(message.productId());
        current.setAmountCents(message.amountCents());
        current.setOriginalAmountCents(message.originalAmountCents());
        current.setValidFrom(message.validFrom());
    }
}
