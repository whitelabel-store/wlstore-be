package com.wlstore.pricing_service.service;

import java.util.Set;
import java.util.UUID;

public interface SkuSyncService {
    void sync(UUID productId, Set<String> currentSkus);
}
