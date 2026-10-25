package com.wlstore.pricing_service.service;

import com.wlstore.pricing_service.model.Price;

import java.util.Collection;
import java.util.List;

public interface PriceService {
    Price definePrice(String sku, long amountCents, Long originalAmountCents);
    List<Price> findCurrentPrices(Collection<String> skus);
    List<Price> findHistory(String sku);
}
