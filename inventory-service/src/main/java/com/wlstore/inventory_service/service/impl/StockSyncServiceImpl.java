package com.wlstore.inventory_service.service.impl;

import com.wlstore.inventory_service.model.Stock;
import com.wlstore.inventory_service.repository.StockRepository;
import com.wlstore.inventory_service.service.StockSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class StockSyncServiceImpl implements StockSyncService {
    private final StockRepository stockRepository;

    @Override
    @Transactional
    public void ensureStockFor(Set<String> skus) {
        for (String sku : skus) {
            if (stockRepository.findBySku(sku).isEmpty()) {
                stockRepository.save(Stock.builder().sku(sku).build());
            }
        }
    }
}
