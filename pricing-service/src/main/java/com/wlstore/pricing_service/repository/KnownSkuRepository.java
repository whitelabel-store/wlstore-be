package com.wlstore.pricing_service.repository;

import com.wlstore.pricing_service.model.KnownSku;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KnownSkuRepository extends JpaRepository<KnownSku, UUID> {
    List<KnownSku> findByProductId(UUID productId);
    boolean existsBySku(String sku);
    Optional<KnownSku> findBySku(String sku);
}
