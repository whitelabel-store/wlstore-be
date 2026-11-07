package com.wlstore.inventory_service.controller;

import com.wlstore.inventory_service.dto.OrderReservationRequestDTO;
import com.wlstore.inventory_service.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservations", description = "Stock held by orders. All the lines of an order are reserved together.")
public class ReservationController {

    private final StockService stockService;

    @Operation(
            summary = "Reserve every line of an order",
            description = "All or nothing: if any line cannot be reserved, nothing is. "
                    + "Idempotent by orderId: sending the same order again answers 204 without reserving twice."
    )
    @ApiResponse(responseCode = "204", description = "Every line reserved (or the order was already reserved)")
    @ApiResponse(responseCode = "400", description = "No orderId, no lines, or a quantity that is not positive")
    @ApiResponse(responseCode = "404", description = "Some SKU is unknown for this tenant")
    @ApiResponse(responseCode = "409", description = "Some line has not enough available stock")
    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reserve(@RequestBody OrderReservationRequestDTO request) {
        stockService.reserveAll(request.orderId(), request.lines());
    }

    @Operation(
            summary = "Release the stock held by an order",
            description = "Gives back every unit the order was holding. Safe to repeat: releasing an order twice, "
                    + "or an order that never reserved anything, changes nothing and answers 204."
    )
    @ApiResponse(responseCode = "204", description = "Units given back (or there was nothing to give back)")
    @PostMapping("/{orderId}/release")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void release(@PathVariable UUID orderId) {
        stockService.release(orderId);
    }

    @Operation(
            summary = "Confirm an order: the held units leave the warehouse",
            description = "On hand and reserved drop together. Safe to repeat: confirming an order twice answers 204 "
                    + "and consumes once. Unlike release, an order that never reserved anything is an error, "
                    + "because a confirmation is a promise that the units were held."
    )
    @ApiResponse(responseCode = "204", description = "Units consumed (or the order was already confirmed)")
    @ApiResponse(responseCode = "404", description = "The order never reserved anything for this tenant")
    @ApiResponse(responseCode = "409", description = "The order was already released: its units went back to the stock")
    @PostMapping("/{orderId}/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirm(@PathVariable UUID orderId) {
        stockService.confirm(orderId);
    }
}