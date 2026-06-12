package com.example.booking.dto;

import com.example.booking.domain.Movie;
import jakarta.validation.constraints.*;

public class MovieDtos {
    public record CreateMovieRequest(
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 50) String language,
            @NotBlank @Size(max = 100) String genre,
            @NotBlank @Size(max = 10) String rating,
            @Min(1) int durationMinutes,
            @Size(max = 2000) String synopsis
    ) {}

    public record MovieResponse(Long id, String title, String language, String genre, String rating,
                                int durationMinutes, String synopsis) {
        public static MovieResponse from(Movie m) {
            return new MovieResponse(m.getId(), m.getTitle(), m.getLanguage(), m.getGenre(),
                    m.getRating(), m.getDurationMinutes(), m.getSynopsis());
        }
    }
}
