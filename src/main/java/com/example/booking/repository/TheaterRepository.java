package com.example.booking.repository;

import com.example.booking.domain.Theater;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TheaterRepository extends JpaRepository<Theater, Long> {
    List<Theater> findAllByCityId(Long cityId);
}
