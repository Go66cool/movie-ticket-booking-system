package com.example.booking.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

public class HoldDtos {
    public record CreateHoldRequest(
            @NotNull Long showId,
            @NotEmpty List<Long> seatIds
    ) {}

    public record HoldResponse(Long id, Long showId, List<Long> showSeatIds, Instant expiresAt, String status) {}
}
