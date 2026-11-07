package com.wlstore.inventory_service.service.impl;

import com.wlstore.common.exception.ResourceNotFoundException;
import com.wlstore.inventory_service.dto.ReservationLineDTO;
import com.wlstore.inventory_service.dto.StockResponseDTO;
import com.wlstore.inventory_service.enums.ReservationStatus;
import com.wlstore.inventory_service.exception.InsufficientStockException;
import com.wlstore.inventory_service.exception.ReservationStateException;
import com.wlstore.inventory_service.exception.StockBelowReservedException;
import com.wlstore.inventory_service.model.Reservation;
import com.wlstore.inventory_service.model.ReservationLine;
import com.wlstore.inventory_service.model.Stock;
import com.wlstore.inventory_service.outbox.StockEventRecorder;
import com.wlstore.inventory_service.repository.ReservationLineRepository;
import com.wlstore.inventory_service.repository.ReservationRepository;
import com.wlstore.inventory_service.repository.StockRepository;
import com.wlstore.inventory_service.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private final StockRepository stockRepository;
    private final StockEventRecorder stockEventRecorder;
    private final ReservationRepository reservationRepository;
    private final TransactionTemplate transactionTemplate;
    private final ReservationLineRepository reservationLineRepository;

    @Override
    @Transactional
    public void reserve(String sku, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        if (stockRepository.reserve(sku, quantity, Instant.now()) == 1) {
            // The UPDATE keeps the row locked until this transaction commits, so this read sees exactly the
            // balance our change produced, and the version in the event follows the order of the commits.
            stockEventRecorder.record(findOrThrow(sku));
            return;
        }

        // Nothing was updated: either the SKU has no stock row (404) or there is not enough available (409).
        // The extra read only serves the error message; the decision was already made atomically by the UPDATE.
        Stock stock = findOrThrow(sku);

        throw new InsufficientStockException(sku, quantity, stock.available());
    }

    @Override
    public void reserveAll(UUID orderId, List<ReservationLineDTO> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("An order needs at least one line");
        }

        if (orderId == null) {
            throw new IllegalArgumentException("An order needs an orderId");
        }

        try {
            transactionTemplate.executeWithoutResult(status -> reserveAllInTransaction(orderId, lines));
        } catch (DataIntegrityViolationException lostTheRace) {
            // Another call for the same order committed first (unique index). That transaction is over, so a
            // second attempt, in a NEW transaction, finds the registry and returns. If the violation was something
            // else, the second attempt fails with it again and the caller sees the real error.
            transactionTemplate.executeWithoutResult(status -> reserveAllInTransaction(orderId, lines));
        }
    }

    private void reserveAllInTransaction(UUID orderId, List<ReservationLineDTO> lines) {
        // retry of an order that already reserved: nothing to do (the first call's answer was just lost)
        if (reservationRepository.existsByOrderId(orderId)) {
            return;
        }

        // registered and flushed BEFORE the lines: a concurrent duplicate waits on the unique index
        // (tenant, order) and fails right away, instead of reserving a second time
        Reservation reservation = reservationRepository.saveAndFlush(Reservation.builder().orderId(orderId).build());

        for (ReservationLineDTO line : lines) {
            reserve(line.sku(), line.quantity());
            reservationLineRepository.save(ReservationLine.builder()
                    .reservationId(reservation.getId())
                    .sku(line.sku())
                    .quantity(line.quantity())
                    .build());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public StockResponseDTO get(String sku) {
        return StockResponseDTO.from(findOrThrow(sku));
    }

    @Override
    @Transactional
    public StockResponseDTO setOnHand(String sku, int quantityOnHand) {
        if (quantityOnHand < 0) {
            throw new IllegalArgumentException("Quantity on hand cannot be negative");
        }

        if (stockRepository.setOnHand(sku, quantityOnHand, Instant.now()) == 1) {
            Stock stock = findOrThrow(sku);
            stockEventRecorder.record(stock);
            return StockResponseDTO.from(stock);
        }

        // Same pattern as reserve: the UPDATE decided; the read only explains why nothing changed.
        Stock stock = findOrThrow(sku);

        throw new StockBelowReservedException(sku, quantityOnHand, stock.getQuantityReserved());
    }

    @Override
    @Transactional
    public void release(UUID orderId) {
        Optional<Reservation> reservation = reservationRepository.findByOrderId(orderId);
        if (reservation.isEmpty()) {
            return;     // never reserved anything: the desired end state already holds
        }

        // Claim the transition RESERVED -> RELEASED in one statement. Only the call that wins it gives units back;
        // a repeat (or a concurrent release) finds it already moved and does nothing.
        if (reservationRepository.transition(orderId, ReservationStatus.RESERVED, ReservationStatus.RELEASED,
                Instant.now()) == 0) {
            return;
        }

        for (ReservationLine line : reservationLineRepository.findByReservationId(reservation.get().getId())) {
            if (stockRepository.release(line.getSku(), line.getQuantity(), Instant.now()) != 1) {
                // would mean the books do not add up; abort the whole release (rolls back the transition too)
                throw new IllegalStateException("Reservation %s holds %d of %s but the stock has fewer reserved"
                        .formatted(orderId, line.getQuantity(), line.getSku()));
            }

            // the snapshot is read AFTER the update, in the same transaction: it is the balance this release left
            stockEventRecorder.record(findOrThrow(line.getSku()));
        }
    }

    @Override
    @Transactional
    public void confirm(UUID orderId) {
        Reservation reservation = reservationRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", "orderId", orderId));

        // Claim the transition RESERVED -> CONFIRMED in one statement. Only the call that wins it consumes the units.
        if (reservationRepository.transition(orderId, ReservationStatus.RESERVED, ReservationStatus.CONFIRMED,
                Instant.now()) == 0) {
            // Lost the claim. Why? Ask the database again: another transaction may have changed the state since
            // our first read, so the entity read at the top of the method may be stale.
            ReservationStatus current = reservationRepository.statusOf(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Reservation", "orderId", orderId));

            if (current == ReservationStatus.CONFIRMED) {
                return;     // a repeat of a confirmation that already happened
            }
            throw new ReservationStateException(orderId, current);
        }

        for (ReservationLine line : reservationLineRepository.findByReservationId(reservation.getId())) {
            if (stockRepository.consume(line.getSku(), line.getQuantity(), Instant.now()) != 1) {
                // would mean the books do not add up; abort the whole confirmation (rolls back the transition too)
                throw new IllegalStateException("Reservation %s holds %d of %s but the stock has fewer reserved"
                        .formatted(orderId, line.getQuantity(), line.getSku()));
            }

            // the snapshot is read AFTER the update, in the same transaction: it is the balance this confirmation left
            stockEventRecorder.record(findOrThrow(line.getSku()));
        }
    }

    private Stock findOrThrow(String sku) {
        return stockRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Stock", "sku", sku));
    }
}