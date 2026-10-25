package com.wlstore.pricing_service.service.impl;

import com.wlstore.pricing_service.exception.UnknownSkuException;
import com.wlstore.pricing_service.model.KnownSku;
import com.wlstore.pricing_service.model.Price;
import com.wlstore.pricing_service.outbox.PriceEventRecorder;
import com.wlstore.pricing_service.repository.KnownSkuRepository;
import com.wlstore.pricing_service.repository.PriceRepository;
import com.wlstore.pricing_service.service.PriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PriceServiceImpl implements PriceService {

    private final PriceRepository priceRepository;
    private final KnownSkuRepository knownSkuRepository;
    private final PriceEventRecorder priceEventRecorder;

    @Override
    @Transactional
    public Price definePrice(String sku, long amountCents, Long originalAmountCents) {
        KnownSku knownSku = knownSkuRepository.findBySku(sku)
                .orElseThrow(() -> new UnknownSkuException(sku));

        Optional<Price> current = priceRepository.findBySkuAndValidToIsNull(sku);

        if (current.isPresent() && isSamePrice(current.get(), amountCents, originalAmountCents)) {
            return current.get();
        }

        Instant now = Instant.now();

        current.ifPresent(price -> {
            price.setValidTo(now);
            priceRepository.saveAndFlush(price);
        });

        Price price = Price.builder()
                .sku(sku)
                .amountCents(amountCents)
                .originalAmountCents(originalAmountCents)
                .validFrom(now)
                .build();

        Price saved = priceRepository.saveAndFlush(price);

        priceEventRecorder.record(saved, knownSku.getProductId());

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Price> findCurrentPrices(Collection<String> skus) {
        if (skus.isEmpty()) {
            return List.of();
        }

        return priceRepository.findBySkuInAndValidToIsNull(skus);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Price> findHistory(String sku) {
        if (!knownSkuRepository.existsBySku(sku)) {
            throw new UnknownSkuException(sku);
        }

        return priceRepository.findBySkuOrderByValidFromAsc(sku);
    }

    private boolean isSamePrice(Price price, long amountCents, Long originalAmountCents) {
        return Objects.equals(price.getAmountCents(), amountCents)
                && Objects.equals(price.getOriginalAmountCents(), originalAmountCents);
    }
}