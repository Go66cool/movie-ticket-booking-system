package com.example.booking.dto;

import com.example.booking.domain.PricingTier;
import com.example.booking.domain.SeatCategory;
import com.example.booking.domain.ShowType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class PricingTierDtos {
    public record CreatePricingTierRequest(
            @NotBlank @Size(max = 100) String name,
            ShowType showType,
            SeatCategory seatCategory,
            @NotNull @DecimalMin("0.01") BigDecimal multiplier
    ) {}

    public record PricingTierResponse(Long id, String name, ShowType showType, SeatCategory seatCategory, BigDecimal multiplier) {
        public static PricingTierResponse from(PricingTier t) {
            return new PricingTierResponse(t.getId(), t.getName(), t.getShowType(), t.getSeatCategory(), t.getMultiplier());
        }
    }
}
