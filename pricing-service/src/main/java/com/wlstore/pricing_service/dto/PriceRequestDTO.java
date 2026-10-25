package com.wlstore.pricing_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PriceRequestDTO(
        @NotNull
        @Positive
        Long amountCents,

        @Positive
        Long originalAmountCents
) {
    @JsonIgnore
    @AssertTrue(message = "originalAmountCents must be greater than amountCents")
    public boolean isOriginalAmountGreaterThanAmount() {
        return originalAmountCents == null
                || amountCents == null
                || originalAmountCents > amountCents;
    }
}
