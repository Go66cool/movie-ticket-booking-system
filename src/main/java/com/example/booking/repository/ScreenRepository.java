package com.example.booking.repository;

import com.example.booking.domain.Screen;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScreenRepository extends JpaRepository<Screen, Long> {
    List<Screen> findAllByTheaterId(Long theaterId);
}
