package com.example.booking.service;

import com.example.booking.domain.City;
import com.example.booking.dto.CityDtos;
import com.example.booking.exception.NotFoundException;
import com.example.booking.repository.CityRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CityService {

    private final CityRepository cityRepository;

    public CityService(CityRepository cityRepository) {
        this.cityRepository = cityRepository;
    }

    @Transactional
    public CityDtos.CityResponse create(CityDtos.CreateCityRequest req) {
        City c = cityRepository.save(City.builder().name(req.name()).state(req.state()).build());
        return CityDtos.CityResponse.from(c);
    }

    @Transactional(readOnly = true)
    public List<CityDtos.CityResponse> list() {
        return cityRepository.findAll().stream().map(CityDtos.CityResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CityDtos.CityResponse get(Long id) {
        City c = cityRepository.findById(id).orElseThrow(() -> new NotFoundException("City " + id));
        return CityDtos.CityResponse.from(c);
    }
}
