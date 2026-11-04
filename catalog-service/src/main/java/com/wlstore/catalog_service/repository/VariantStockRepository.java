package com.wlstore.catalog_service.repository;

import com.wlstore.catalog_service.model.VariantStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VariantStockRepository extends JpaRepository<VariantStock, UUID> {
    Optional<VariantStock> findBySku(String sku);
}
