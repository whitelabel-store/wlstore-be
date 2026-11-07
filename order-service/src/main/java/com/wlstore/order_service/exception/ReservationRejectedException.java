package com.wlstore.order_service.exception;

/**
 * O inventory recusou a reserva (sem estoque suficiente, SKU desconhecido...).
 * É uma recusa de negócio: o pedido vira REJECTED, e não fica esperando nova tentativa.
 */
public class ReservationRejectedException extends RuntimeException {

    public ReservationRejectedException(String message) {
        super(message);
    }
}