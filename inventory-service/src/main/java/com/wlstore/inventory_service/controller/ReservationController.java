package com.wlstore.inventory_service.controller;

import com.wlstore.inventory_service.dto.OrderReservationRequestDTO;
import com.wlstore.inventory_service.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
    @ApiResponse(responseCode = "400", description = "No lines, or a quantity that is not positive")
    @ApiResponse(responseCode = "404", description = "Some SKU is unknown for this tenant")
    @ApiResponse(responseCode = "409", description = "Some line has not enough available stock")
    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reserve(@RequestBody OrderReservationRequestDTO request) {
        stockService.reserveAll(request.orderId(), request.lines());
    }
}
