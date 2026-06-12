package com.example.booking.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class BookingDtos {

    public record CreateBookingRequest(
            @NotNull Long holdId,
            @Size(max = 50) String discountCode
    ) {}

    public record PayRequest(
            @Size(max = 50) String method,
            String mockOutcome
    ) {}

    public record BookingSeatDto(Long showSeatId, String rowLabel, int number, String category, BigDecimal price) {}

    public record BookingResponse(
            Long id, String reference, Long userId, Long showId, String status,
            BigDecimal subtotalAmount, BigDecimal discountAmount, BigDecimal totalAmount,
            String discountCode, Instant createdAt, Instant confirmedAt, Instant cancelledAt,
            List<BookingSeatDto> seats
    ) {}

    public record CancellationResponse(String reference, String status, BigDecimal refundAmount, String policyApplied) {}
}
