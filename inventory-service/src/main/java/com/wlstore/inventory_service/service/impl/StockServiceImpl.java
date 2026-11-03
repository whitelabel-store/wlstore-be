package com.wlstore.inventory_service.service.impl;

import com.wlstore.common.exception.ResourceNotFoundException;
import com.wlstore.inventory_service.dto.StockResponseDTO;
import com.wlstore.inventory_service.exception.InsufficientStockException;
import com.wlstore.inventory_service.exception.StockBelowReservedException;
import com.wlstore.inventory_service.model.Stock;
import com.wlstore.inventory_service.repository.StockRepository;
import com.wlstore.inventory_service.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

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

        if (stockRepository.reserve(sku, quantity, Instant.now()) == 1) {
            return;
        }

        // Nothing was updated: either the SKU has no stock row (404) or there is not enough available (409).
        // The extra read only serves the error message; the decision was already made atomically by the UPDATE.
        Stock stock = findOrThrow(sku);

        throw new InsufficientStockException(sku, quantity, stock.available());
    }

    @Override
    @Transactional(readOnly = true)
    public StockResponseDTO get(String sku) {
        return StockResponseDTO.from(findOrThrow(sku));
    }

    @Override
    @Transactional
    public StockResponseDTO setOnHand(String sku, int quantityOnHand) {
        if (quantityOnHand < 0) {
            throw new IllegalArgumentException("Quantity on hand cannot be negative");
        }

        if (stockRepository.setOnHand(sku, quantityOnHand, Instant.now()) == 1) {
            return StockResponseDTO.from(findOrThrow(sku));
        }

        // Same pattern as reserve: the UPDATE decided; the read only explains why nothing changed.
        Stock stock = findOrThrow(sku);

        throw new StockBelowReservedException(sku, quantityOnHand, stock.getQuantityReserved());
    }

    private Stock findOrThrow(String sku) {
        return stockRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Stock", "id", sku));
    }
}