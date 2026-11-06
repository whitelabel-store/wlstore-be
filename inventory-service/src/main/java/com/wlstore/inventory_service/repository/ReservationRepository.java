package com.wlstore.inventory_service.repository;

import com.wlstore.inventory_service.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    // the tenant predicate is added by Hibernate (@TenantId)
    boolean existsByOrderId(UUID orderId);
}
