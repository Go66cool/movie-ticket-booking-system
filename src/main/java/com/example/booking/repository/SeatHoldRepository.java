package com.example.booking.repository;

import com.example.booking.domain.SeatHold;
import com.example.booking.domain.SeatHoldStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatHoldRepository extends JpaRepository<SeatHold, Long> {
    List<SeatHold> findAllByStatusAndExpiresAtBefore(SeatHoldStatus status, Instant cutoff);
    List<SeatHold> findAllByUserIdAndStatus(Long userId, SeatHoldStatus status);
}
