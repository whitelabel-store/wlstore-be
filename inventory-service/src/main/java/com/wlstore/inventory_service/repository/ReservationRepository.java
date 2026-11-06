package com.wlstore.inventory_service.repository;

import com.wlstore.inventory_service.enums.ReservationStatus;
import com.wlstore.inventory_service.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    // the tenant predicate is added by Hibernate (@TenantId)
    boolean existsByOrderId(UUID orderId);

    Optional<Reservation> findByOrderId(UUID orderId);

    /**
     * Moves the reservation from one state to another, but only if it is still in the expected one.
     * Check and write in ONE statement, like the stock updates: of two concurrent releases, only one finds
     * the row in RESERVED. Returns 1 when this call made the transition, 0 when someone else already did.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Reservation r
               set r.status = :to,
                   r.version = r.version + 1,
                   r.updatedAt = :now
             where r.orderId = :orderId
               and r.status = :from
            """)
    int transition(@Param("orderId") UUID orderId,
                   @Param("from") ReservationStatus from,
                   @Param("to") ReservationStatus to,
                   @Param("now") Instant now);
}