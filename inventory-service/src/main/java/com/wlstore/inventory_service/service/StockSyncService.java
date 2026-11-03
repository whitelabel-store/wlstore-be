package com.wlstore.inventory_service.service;

import java.util.Set;

public interface StockSyncService {

    /**
     * Makes sure every SKU has a stock row for the current tenant.
     * New SKUs start with zero on hand and zero reserved; existing rows are never touched.
     */
    void ensureStockFor(Set<String> skus);
}
