package com.wlstore.inventory_service.controller;

import com.wlstore.inventory_service.dto.ReserveStockRequestDTO;
import com.wlstore.inventory_service.dto.SetOnHandRequestDTO;
import com.wlstore.inventory_service.dto.StockResponseDTO;
import com.wlstore.inventory_service.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stock")
@RequiredArgsConstructor
@Tag(name = "Stock", description = "Stock balance per SKU. SKUs are created from catalog events with zero stock.")
public class StockController {

    private final StockService stockService;

    @Operation(summary = "Get the stock balance of a SKU")
    @ApiResponse(responseCode = "200", description = "Balance with on hand, reserved and available")
    @ApiResponse(responseCode = "404", description = "Unknown SKU for this tenant")
    @GetMapping("/{sku}")
    public StockResponseDTO get(@PathVariable String sku) {
        return stockService.get(sku);
    }

    @Operation(
            summary = "Set the physical balance of a SKU",
            description = "Replaces quantityOnHand with an absolute value. Existing reservations are kept, "
                    + "so the value cannot be lower than the reserved units. Unknown SKUs are not created here: "
                    + "they come from catalog events."
    )
    @ApiResponse(responseCode = "200", description = "Balance updated")
    @ApiResponse(responseCode = "400", description = "Negative quantity")
    @ApiResponse(responseCode = "404", description = "Unknown SKU for this tenant")
    @ApiResponse(responseCode = "409", description = "Value is lower than the reserved units")
    @PutMapping("/{sku}")
    public StockResponseDTO setOnHand(@PathVariable String sku, @RequestBody SetOnHandRequestDTO request) {
        return stockService.setOnHand(sku, request.quantityOnHand());
    }

    @Operation(
            summary = "Reserve units of a SKU",
            description = "Atomically holds the units if enough are available. Never goes below zero available."
    )
    @ApiResponse(responseCode = "204", description = "Units reserved")
    @ApiResponse(responseCode = "400", description = "Quantity is not positive")
    @ApiResponse(responseCode = "404", description = "Unknown SKU for this tenant")
    @ApiResponse(responseCode = "409", description = "Not enough available stock")
    @PostMapping("/{sku}/reservations")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reserve(@PathVariable String sku, @RequestBody ReserveStockRequestDTO request) {
        stockService.reserve(sku, request.quantity());
    }
}