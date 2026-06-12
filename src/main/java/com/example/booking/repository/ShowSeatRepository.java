package com.example.booking.repository;

import com.example.booking.domain.ShowSeat;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShowSeatRepository extends JpaRepository<ShowSeat, Long> {

    List<ShowSeat> findAllByShowId(Long showId);

    List<ShowSeat> findAllByHoldId(Long holdId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ss from ShowSeat ss where ss.id in :ids order by ss.id")
    List<ShowSeat> lockByIds(@Param("ids") Collection<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ss from ShowSeat ss where ss.show.id = :showId and ss.seat.id in :seatIds order by ss.id")
    List<ShowSeat> lockByShowAndSeats(@Param("showId") Long showId,
                                      @Param("seatIds") Collection<Long> seatIds);
}
