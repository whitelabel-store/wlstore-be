package com.wlstore.pricing_service.repository;

import com.wlstore.pricing_service.model.Price;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PriceRepository extends JpaRepository<Price, UUID> {
    Optional<Price> findBySkuAndValidToIsNull(String sku);
    List<Price> findBySkuOrderByValidFromAsc(String sku);
    List<Price> findBySkuInAndValidToIsNull(Collection<String> skus);
}
