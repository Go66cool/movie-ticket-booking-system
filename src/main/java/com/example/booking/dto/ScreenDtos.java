package com.example.booking.dto;

import com.example.booking.domain.SeatCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public class ScreenDtos {

    public record SeatLayoutEntry(
            @NotBlank @Size(max = 5) String rowLabel,
            @Min(1) int seatsInRow,
            @NotNull SeatCategory category
    ) {}

    public record CreateScreenRequest(
            @NotNull Long theaterId,
            @NotBlank @Size(max = 100) String name,
            @NotEmpty @Valid List<SeatLayoutEntry> layout
    ) {}

    public record SeatDto(Long id, String rowLabel, int number, SeatCategory category) {}

    public record ScreenResponse(Long id, Long theaterId, String name, int totalSeats, List<SeatDto> seats) {}
}
