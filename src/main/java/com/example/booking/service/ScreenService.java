package com.example.booking.service;

import com.example.booking.domain.Screen;
import com.example.booking.domain.Seat;
import com.example.booking.domain.Theater;
import com.example.booking.dto.ScreenDtos;
import com.example.booking.exception.NotFoundException;
import com.example.booking.repository.ScreenRepository;
import com.example.booking.repository.SeatRepository;
import com.example.booking.repository.TheaterRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScreenService {

    private final ScreenRepository screenRepository;
    private final TheaterRepository theaterRepository;
    private final SeatRepository seatRepository;

    public ScreenService(ScreenRepository screenRepository, TheaterRepository theaterRepository,
                         SeatRepository seatRepository) {
        this.screenRepository = screenRepository;
        this.theaterRepository = theaterRepository;
        this.seatRepository = seatRepository;
    }

    @Transactional
    public ScreenDtos.ScreenResponse create(ScreenDtos.CreateScreenRequest req) {
        Theater theater = theaterRepository.findById(req.theaterId())
                .orElseThrow(() -> new NotFoundException("Theater " + req.theaterId()));
        int total = req.layout().stream().mapToInt(ScreenDtos.SeatLayoutEntry::seatsInRow).sum();
        Screen screen = screenRepository.save(Screen.builder()
                .theater(theater).name(req.name()).totalSeats(total).build());

        List<Seat> seats = new ArrayList<>();
        for (ScreenDtos.SeatLayoutEntry row : req.layout()) {
            for (int i = 1; i <= row.seatsInRow(); i++) {
                seats.add(Seat.builder()
                        .screen(screen)
                        .rowLabel(row.rowLabel())
                        .number(i)
                        .category(row.category())
                        .build());
            }
        }
        seatRepository.saveAll(seats);
        return toResponse(screen, seats);
    }

    @Transactional(readOnly = true)
    public ScreenDtos.ScreenResponse get(Long id) {
        Screen s = screenRepository.findById(id).orElseThrow(() -> new NotFoundException("Screen " + id));
        return toResponse(s, seatRepository.findAllByScreenId(id));
    }

    @Transactional(readOnly = true)
    public List<ScreenDtos.ScreenResponse> listByTheater(Long theaterId) {
        return screenRepository.findAllByTheaterId(theaterId).stream()
                .map(s -> toResponse(s, seatRepository.findAllByScreenId(s.getId()))).toList();
    }

    private ScreenDtos.ScreenResponse toResponse(Screen s, List<Seat> seats) {
        List<ScreenDtos.SeatDto> dtos = seats.stream()
                .map(se -> new ScreenDtos.SeatDto(se.getId(), se.getRowLabel(), se.getNumber(), se.getCategory()))
                .toList();
        return new ScreenDtos.ScreenResponse(s.getId(), s.getTheater().getId(), s.getName(), s.getTotalSeats(), dtos);
    }
}
