package com.example.booking.service;

import com.example.booking.domain.Movie;
import com.example.booking.dto.MovieDtos;
import com.example.booking.exception.NotFoundException;
import com.example.booking.repository.MovieRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MovieService {

    private final MovieRepository movieRepository;

    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    @Transactional
    public MovieDtos.MovieResponse create(MovieDtos.CreateMovieRequest r) {
        Movie m = movieRepository.save(Movie.builder()
                .title(r.title()).language(r.language()).genre(r.genre()).rating(r.rating())
                .durationMinutes(r.durationMinutes()).synopsis(r.synopsis()).build());
        return MovieDtos.MovieResponse.from(m);
    }

    @Transactional(readOnly = true)
    public List<MovieDtos.MovieResponse> list() {
        return movieRepository.findAll().stream().map(MovieDtos.MovieResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public MovieDtos.MovieResponse get(Long id) {
        return MovieDtos.MovieResponse.from(movieRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Movie " + id)));
    }
}
