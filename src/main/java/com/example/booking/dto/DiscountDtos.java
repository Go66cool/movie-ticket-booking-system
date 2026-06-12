package com.example.booking.dto;

import com.example.booking.domain.DiscountCode;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

public class DiscountDtos {
    public record CreateDiscountRequest(
            @NotBlank @Size(max = 50) String code,
            @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal percentOff,
            @DecimalMin("0.0") BigDecimal flatOff,
            @DecimalMin("0.0") BigDecimal maxDiscount,
            @NotNull Instant validFrom,
            @NotNull Instant validTo,
            @Min(1) int usageLimit,
            boolean active
    ) {}

    public record DiscountResponse(Long id, String code, BigDecimal percentOff, BigDecimal flatOff,
                                   BigDecimal maxDiscount, Instant validFrom, Instant validTo,
                                   int usageLimit, int usedCount, boolean active) {
        public static DiscountResponse from(DiscountCode d) {
            return new DiscountResponse(d.getId(), d.getCode(), d.getPercentOff(), d.getFlatOff(),
                    d.getMaxDiscount(), d.getValidFrom(), d.getValidTo(),
                    d.getUsageLimit(), d.getUsedCount(), d.isActive());
        }
    }
}
