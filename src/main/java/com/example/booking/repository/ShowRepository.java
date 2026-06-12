package com.example.booking.repository;

import com.example.booking.domain.Show;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShowRepository extends JpaRepository<Show, Long> {

    @Query("""
        select s from Show s
        where s.movie.id = :movieId
          and s.screen.theater.city.id = :cityId
          and s.startTime between :from and :to
        order by s.startTime asc
    """)
    List<Show> searchByMovieAndCity(@Param("movieId") Long movieId,
                                    @Param("cityId") Long cityId,
                                    @Param("from") Instant from,
                                    @Param("to") Instant to);

    @Query("""
        select s from Show s
        where s.screen.id = :screenId
          and s.startTime < :end
          and s.endTime > :start
    """)
    List<Show> findOverlapping(@Param("screenId") Long screenId,
                               @Param("start") Instant start,
                               @Param("end") Instant end);
}
