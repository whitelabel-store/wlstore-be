package com.wlstore.inventory_service.dto;

import java.util.List;
import java.util.UUID;

/**
 * The stock an order wants to hold: every line or none. The orderId makes the call idempotent.
 */
public record OrderReservationRequestDTO(
   UUID orderId,
   List<ReservationLineDTO> lines
) {
}
