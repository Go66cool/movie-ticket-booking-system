package com.example.booking.service;

import com.example.booking.domain.City;
import com.example.booking.domain.Theater;
import com.example.booking.dto.TheaterDtos;
import com.example.booking.exception.NotFoundException;
import com.example.booking.repository.CityRepository;
import com.example.booking.repository.TheaterRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TheaterService {

    private final TheaterRepository theaterRepository;
    private final CityRepository cityRepository;

    public TheaterService(TheaterRepository theaterRepository, CityRepository cityRepository) {
        this.theaterRepository = theaterRepository;
        this.cityRepository = cityRepository;
    }

    @Transactional
    public TheaterDtos.TheaterResponse create(TheaterDtos.CreateTheaterRequest req) {
        City city = cityRepository.findById(req.cityId())
                .orElseThrow(() -> new NotFoundException("City " + req.cityId()));
        Theater t = theaterRepository.save(Theater.builder()
                .city(city).name(req.name()).address(req.address()).build());
        return TheaterDtos.TheaterResponse.from(t);
    }

    @Transactional(readOnly = true)
    public List<TheaterDtos.TheaterResponse> listByCity(Long cityId) {
        return theaterRepository.findAllByCityId(cityId).stream()
                .map(TheaterDtos.TheaterResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<TheaterDtos.TheaterResponse> listAll() {
        return theaterRepository.findAll().stream().map(TheaterDtos.TheaterResponse::from).toList();
    }
}
