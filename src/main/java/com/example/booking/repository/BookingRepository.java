package com.example.booking.repository;

import com.example.booking.domain.Booking;
import com.example.booking.domain.BookingStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByReference(String reference);
    Page<Booking> findAllByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    List<Booking> findAllByStatusAndCreatedAtBefore(BookingStatus status, java.time.Instant cutoff);
}
