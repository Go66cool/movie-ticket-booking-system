package com.example.booking.dto;

import com.example.booking.domain.ShowType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

public class ShowDtos {

    public record CreateShowRequest(
            @NotNull Long movieId,
            @NotNull Long screenId,
            @NotNull @Future Instant startTime,
            @NotNull @DecimalMin("0.01") BigDecimal basePrice,
            @NotNull ShowType showType
    ) {}

    public record ShowResponse(
            Long id, Long movieId, String movieTitle,
            Long screenId, String screenName, Long theaterId, String theaterName, Long cityId, String cityName,
            Instant startTime, Instant endTime, BigDecimal basePrice, ShowType showType
    ) {}

    public record SeatAvailabilityDto(
            Long showSeatId, Long seatId, String rowLabel, int number,
            String category, String status, BigDecimal price
    ) {}
}
