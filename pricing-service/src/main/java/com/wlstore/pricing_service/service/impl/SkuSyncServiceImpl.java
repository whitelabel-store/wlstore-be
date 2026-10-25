package com.wlstore.pricing_service.service.impl;

import com.wlstore.pricing_service.model.KnownSku;
import com.wlstore.pricing_service.repository.KnownSkuRepository;
import com.wlstore.pricing_service.repository.PriceRepository;
import com.wlstore.pricing_service.service.SkuSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SkuSyncServiceImpl implements SkuSyncService {

    private final KnownSkuRepository knownSkuRepository;
    private final PriceRepository priceRepository;

    @Override
    @Transactional
    public void sync(UUID productId, Set<String> currentSkus) {
        List<KnownSku> known = knownSkuRepository.findByProductId(productId);

        Set<String> knownSkus = known.stream()
                .map(KnownSku::getSku)
                .collect(Collectors.toSet());

        List<KnownSku> removed = known.stream()
                .filter(k -> !currentSkus.contains(k.getSku()))
                .toList();

        List<KnownSku> added = currentSkus.stream()
                .filter(sku -> !knownSkus.contains(sku))
                .map(sku -> newKnownSku(productId, sku))
                .toList();

        removed.forEach(k -> closeCurrentPrice(k.getSku()));
        knownSkuRepository.deleteAll(removed);
        knownSkuRepository.saveAll(added);
    }

    private KnownSku newKnownSku(UUID productId, String sku) {
        return KnownSku.builder()
                .productId(productId)
                .sku(sku)
                .build();
    }

    private void closeCurrentPrice(String sku) {
        Instant now = Instant.now();
        priceRepository.findBySkuAndValidToIsNull(sku)
                .ifPresent(price -> price.setValidTo(now));
    }
}