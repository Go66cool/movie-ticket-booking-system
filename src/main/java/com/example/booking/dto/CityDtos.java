package com.example.booking.dto;

import com.example.booking.domain.City;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CityDtos {
    public record CreateCityRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Size(max = 100) String state
    ) {}

    public record CityResponse(Long id, String name, String state) {
        public static CityResponse from(City c) {
            return new CityResponse(c.getId(), c.getName(), c.getState());
        }
    }
}
