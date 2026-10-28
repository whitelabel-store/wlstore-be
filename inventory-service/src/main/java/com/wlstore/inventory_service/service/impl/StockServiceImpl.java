package com.wlstore.inventory_service.service.impl;

import com.wlstore.common.exception.ResourceNotFoundException;
import com.wlstore.inventory_service.exception.InsufficientStockException;
import com.wlstore.inventory_service.model.Stock;
import com.wlstore.inventory_service.repository.StockRepository;
import com.wlstore.inventory_service.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {
    private final StockRepository stockRepository;

    @Override
    @Transactional
    public void reserve(String sku, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        Stock stock = stockRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Stock", "sku", sku));

        if (stock.available() < quantity) {
            throw new InsufficientStockException(sku, quantity, stock.available());
        }

        stock.setQuantityReserved(stock.getQuantityReserved() + quantity);
    }
}
