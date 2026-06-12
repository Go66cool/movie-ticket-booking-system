package com.example.booking.dto;

import com.example.booking.domain.Theater;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class TheaterDtos {
    public record CreateTheaterRequest(
            @NotNull Long cityId,
            @NotBlank @Size(max = 200) String name,
            @NotBlank @Size(max = 400) String address
    ) {}

    public record TheaterResponse(Long id, Long cityId, String cityName, String name, String address) {
        public static TheaterResponse from(Theater t) {
            return new TheaterResponse(t.getId(), t.getCity().getId(), t.getCity().getName(),
                    t.getName(), t.getAddress());
        }
    }
}
