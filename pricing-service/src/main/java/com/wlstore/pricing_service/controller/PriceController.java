package com.wlstore.pricing_service.controller;

import com.wlstore.pricing_service.dto.PriceRequestDTO;
import com.wlstore.pricing_service.dto.PriceResponseDTO;
import com.wlstore.pricing_service.service.PriceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/prices")
@RequiredArgsConstructor
@Tag(name = "Prices", description = "Current and historical pricing per SKU. SKUs are synced from the catalog service; defining a price for an unknown SKU returns 404.")
public class PriceController {
    private final PriceService priceService;

    private static final String PRICE_REQUEST_EXAMPLE = """
            {
              "amountCents": 9000,
              "originalAmountCents": 10000
            }
            """;

    @Operation(
            summary = "Define the current price for a SKU",
            description = """
                    Sets the price in effect starting now. If the SKU already has a current price, it is closed \
                    (its validTo is set to now) and the new one becomes current.

                    `originalAmountCents` is optional and, when present, must be greater than `amountCents` \
                    (used to display a "from/to" discount).
                    """
    )
    @ApiResponse(responseCode = "200", description = "Price defined")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "404", description = "sku is not a known SKU")
    @PutMapping("/{sku}")
    @ResponseStatus(HttpStatus.OK)
    public PriceResponseDTO define(
            @PathVariable String sku,
            @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "New price payload", required = true,
                    content = @Content(schema = @Schema(implementation = PriceRequestDTO.class),
                            examples = @ExampleObject(name = "New price", value = PRICE_REQUEST_EXAMPLE)))
            @RequestBody PriceRequestDTO request
    ) {
        return PriceResponseDTO.from(
                priceService.definePrice(sku, request.amountCents(), request.originalAmountCents()));
    }

    @Operation(
            summary = "List the current prices of several SKUs",
            description = """
                    Returns the price in effect for each requested SKU, in a single call. \
                    SKUs without a current price (not priced yet, or unknown to this tenant) are simply \
                    absent from the response; this is not an error.
                    """
    )
    @ApiResponse(responseCode = "200", description = "Current prices found (possibly empty)")
    @GetMapping
    public List<PriceResponseDTO> listCurrent(
            @Parameter(description = "Comma-separated SKUs", example = "CAM-P-AZUL,CAM-M-AZUL")
            @RequestParam List<String> skus){
        return priceService.findCurrentPrices(skus)
                .stream()
                .map(PriceResponseDTO::from)
                .toList();
    }

    @Operation(
            summary = "Price history of a SKU",
            description = """
                    Returns every price period of the SKU, oldest first. The period in effect is the one \
                    whose `validTo` is null. A known SKU that was never priced returns an empty list.
                    """
    )
    @ApiResponse(responseCode = "200", description = "History found (possibly empty)")
    @ApiResponse(responseCode = "404", description = "sku is not a known SKU")
    @GetMapping("/{sku}/history")
    public List<PriceResponseDTO> history(@PathVariable String sku) {
        return priceService.findHistory(sku)
                .stream()
                .map(PriceResponseDTO::from)
                .toList();
    }
}
