package com.wlstore.inventory_service.exception;

import com.wlstore.inventory_service.enums.ReservationStatus;
import lombok.Getter;

import java.util.UUID;

/**
 * The operation does not make sense for the state the reservation is in (for example, confirming an order whose
 * units were already given back). Answered as 409: the request is fine, the resource's current state is not.
 */
@Getter
public class ReservationStateException extends RuntimeException {

    private final UUID orderId;
    private final ReservationStatus status;

    public ReservationStateException(UUID orderId, ReservationStatus status) {
        super("Reservation of order %s is %s".formatted(orderId, status));
        this.orderId = orderId;
        this.status = status;
    }
}