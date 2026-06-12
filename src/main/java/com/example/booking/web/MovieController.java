package com.example.booking.web;

import com.example.booking.dto.MovieDtos;
import com.example.booking.service.MovieService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping
    public List<MovieDtos.MovieResponse> list() {
        return movieService.list();
    }

    @GetMapping("/{id}")
    public MovieDtos.MovieResponse get(@PathVariable Long id) {
        return movieService.get(id);
    }
}
