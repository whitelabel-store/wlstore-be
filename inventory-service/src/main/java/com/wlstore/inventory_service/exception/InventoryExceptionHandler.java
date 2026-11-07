package com.wlstore.inventory_service.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

/**
 * Inventory-specific error mapping. Runs BEFORE the shared GlobalControllerAdvice, whose catch-all
 * handler for Exception would otherwise be allowed to answer 500 for our domain exceptions.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class InventoryExceptionHandler {

    @ExceptionHandler(InsufficientStockException.class)
    public ProblemDetail handleInsufficientStock(InsufficientStockException ex) {

        log.warn("Insufficient stock: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("Insufficient Stock");
        problemDetail.setType(URI.create("https://example.com/insufficient-stock"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("sku", ex.getSku());
        problemDetail.setProperty("requested", ex.getRequested());
        problemDetail.setProperty("available", ex.getAvailable());

        return problemDetail;
    }

    @ExceptionHandler(StockBelowReservedException.class)
    public ProblemDetail handleStockBelowReserved(StockBelowReservedException ex) {

        log.warn("Stock below reserved: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("Stock Below Reserved");
        problemDetail.setType(URI.create("https://example.com/stock-below-reserved"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("sku", ex.getSku());
        problemDetail.setProperty("requestedOnHand", ex.getRequestedOnHand());
        problemDetail.setProperty("reserved", ex.getReserved());

        return problemDetail;
    }

    @ExceptionHandler(ReservationStateException.class)
    public ProblemDetail handleReservationState(ReservationStateException ex) {

        log.warn("Reservation state conflict: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("Reservation State Conflict");
        problemDetail.setType(URI.create("https://example.com/reservation-state"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("orderId", ex.getOrderId());
        problemDetail.setProperty("status", ex.getStatus());

        return problemDetail;
    }
}