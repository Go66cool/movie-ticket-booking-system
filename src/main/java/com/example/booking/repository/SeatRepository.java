package com.example.booking.repository;

import com.example.booking.domain.Seat;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findAllByScreenId(Long screenId);
}
