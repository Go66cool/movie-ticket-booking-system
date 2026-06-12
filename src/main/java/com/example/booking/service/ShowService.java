package com.example.booking.service;

import com.example.booking.domain.*;
import com.example.booking.dto.ShowDtos;
import com.example.booking.exception.BadRequestException;
import com.example.booking.exception.ConflictException;
import com.example.booking.exception.NotFoundException;
import com.example.booking.repository.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShowService {

    private final ShowRepository showRepository;
    private final MovieRepository movieRepository;
    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;
    private final ShowSeatRepository showSeatRepository;
    private final PricingService pricingService;

    public ShowService(ShowRepository showRepository, MovieRepository movieRepository,
                       ScreenRepository screenRepository, SeatRepository seatRepository,
                       ShowSeatRepository showSeatRepository, PricingService pricingService) {
        this.showRepository = showRepository;
        this.movieRepository = movieRepository;
        this.screenRepository = screenRepository;
        this.seatRepository = seatRepository;
        this.showSeatRepository = showSeatRepository;
        this.pricingService = pricingService;
    }

    @Transactional
    public ShowDtos.ShowResponse create(ShowDtos.CreateShowRequest req) {
        Movie movie = movieRepository.findById(req.movieId())
                .orElseThrow(() -> new NotFoundException("Movie " + req.movieId()));
        Screen screen = screenRepository.findById(req.screenId())
                .orElseThrow(() -> new NotFoundException("Screen " + req.screenId()));

        Instant end = req.startTime().plus(movie.getDurationMinutes(), ChronoUnit.MINUTES);
        if (!showRepository.findOverlapping(screen.getId(), req.startTime(), end).isEmpty()) {
            throw new ConflictException("Screen has an overlapping show at the requested time");
        }
        if (req.basePrice().signum() <= 0) throw new BadRequestException("basePrice must be > 0");

        Show show = showRepository.save(Show.builder()
                .movie(movie).screen(screen).startTime(req.startTime()).endTime(end)
                .basePrice(req.basePrice()).showType(req.showType()).build());

        List<Seat> seats = seatRepository.findAllByScreenId(screen.getId());
        List<ShowSeat> showSeats = new ArrayList<>(seats.size());
        for (Seat s : seats) {
            showSeats.add(ShowSeat.builder()
                    .show(show).seat(s).status(ShowSeatStatus.AVAILABLE)
                    .price(pricingService.priceFor(req.basePrice(), req.showType(), s.getCategory()))
                    .build());
        }
        showSeatRepository.saveAll(showSeats);
        return toResponse(show);
    }

    @Transactional(readOnly = true)
    public List<ShowDtos.ShowResponse> search(Long movieId, Long cityId, Instant from, Instant to) {
        Instant f = from != null ? from : Instant.now();
        Instant t = to != null ? to : f.plus(30, ChronoUnit.DAYS);
        return showRepository.searchByMovieAndCity(movieId, cityId, f, t).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ShowDtos.ShowResponse get(Long id) {
        return toResponse(showRepository.findById(id).orElseThrow(() -> new NotFoundException("Show " + id)));
    }

    @Transactional(readOnly = true)
    public List<ShowDtos.SeatAvailabilityDto> seatMap(Long showId) {
        if (!showRepository.existsById(showId)) throw new NotFoundException("Show " + showId);
        return showSeatRepository.findAllByShowId(showId).stream()
                .map(ss -> new ShowDtos.SeatAvailabilityDto(
                        ss.getId(), ss.getSeat().getId(), ss.getSeat().getRowLabel(),
                        ss.getSeat().getNumber(), ss.getSeat().getCategory().name(),
                        ss.getStatus().name(), ss.getPrice()))
                .toList();
    }

    private ShowDtos.ShowResponse toResponse(Show s) {
        Screen sc = s.getScreen();
        Theater th = sc.getTheater();
        City ci = th.getCity();
        return new ShowDtos.ShowResponse(
                s.getId(), s.getMovie().getId(), s.getMovie().getTitle(),
                sc.getId(), sc.getName(), th.getId(), th.getName(), ci.getId(), ci.getName(),
                s.getStartTime(), s.getEndTime(), s.getBasePrice(), s.getShowType());
    }
}
