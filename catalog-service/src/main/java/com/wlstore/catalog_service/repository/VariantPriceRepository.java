package com.wlstore.catalog_service.repository;

import com.wlstore.catalog_service.model.VariantPrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VariantPriceRepository extends JpaRepository<VariantPrice, UUID> {
    Optional<VariantPrice> findBySku(String sku);
    List<VariantPrice> findByProductId(UUID productId);
    List<VariantPrice> findByProductIdIn(Collection<UUID> productIds);
}
