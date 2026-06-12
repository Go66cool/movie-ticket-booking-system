package com.example.booking.web;

import com.example.booking.dto.CityDtos;
import com.example.booking.dto.TheaterDtos;
import com.example.booking.service.CityService;
import com.example.booking.service.TheaterService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cities")
public class CityController {

    private final CityService cityService;
    private final TheaterService theaterService;

    public CityController(CityService cityService, TheaterService theaterService) {
        this.cityService = cityService;
        this.theaterService = theaterService;
    }

    @GetMapping
    public List<CityDtos.CityResponse> list() {
        return cityService.list();
    }

    @GetMapping("/{id}")
    public CityDtos.CityResponse get(@PathVariable Long id) {
        return cityService.get(id);
    }

    @GetMapping("/{id}/theaters")
    public List<TheaterDtos.TheaterResponse> theaters(@PathVariable Long id) {
        return theaterService.listByCity(id);
    }
}
