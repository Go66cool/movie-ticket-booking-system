package com.example.booking.dto;

import com.example.booking.domain.RefundPolicy;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class RefundPolicyDtos {
    public record CreatePolicyRequest(
            @NotBlank @Size(max = 100) String name,
            @Min(0) int hoursBeforeShow,
            @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal refundPercent,
            boolean active
    ) {}

    public record PolicyResponse(Long id, String name, int hoursBeforeShow, BigDecimal refundPercent, boolean active) {
        public static PolicyResponse from(RefundPolicy p) {
            return new PolicyResponse(p.getId(), p.getName(), p.getHoursBeforeShow(), p.getRefundPercent(), p.isActive());
        }
    }
}
