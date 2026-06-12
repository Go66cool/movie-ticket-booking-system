package com.example.booking.web;

import com.example.booking.dto.ShowDtos;
import com.example.booking.service.ShowService;
import java.time.Instant;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shows")
public class ShowController {

    private final ShowService showService;

    public ShowController(ShowService showService) {
        this.showService = showService;
    }

    @GetMapping("/search")
    public List<ShowDtos.ShowResponse> search(
            @RequestParam Long movieId,
            @RequestParam Long cityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return showService.search(movieId, cityId, from, to);
    }

    @GetMapping("/{id}")
    public ShowDtos.ShowResponse get(@PathVariable Long id) {
        return showService.get(id);
    }

    @GetMapping("/{id}/seats")
    public List<ShowDtos.SeatAvailabilityDto> seats(@PathVariable Long id) {
        return showService.seatMap(id);
    }
}
