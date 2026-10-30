package com.wlstore.inventory_service.repository;

import com.wlstore.inventory_service.model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface StockRepository extends JpaRepository<Stock, UUID> {
    Optional<Stock> findBySku(String sku);

    /**
     * Check and write in ONE statement: the row is locked while it runs, and a waiting transaction re-evaluates the
     * WHERE against the committed value. Returns 1 when reserved, 0 when there was not enough available (or no such SKU).
     * The version is bumped by hand so other optimistic writers still notice this change.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Stock s
               set s.quantityReserved = s.quantityReserved + :quantity,
                   s.version = s.version + 1,
                   s.updatedAt = :now
             where s.sku = :sku
               and s.quantityOnHand - s.quantityReserved >= :quantity
            """)
    int reserve(@Param("sku") String sku, @Param("quantity") int quantity, @Param("now") Instant now);
}
