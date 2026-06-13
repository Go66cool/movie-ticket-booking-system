package com.example.booking.service;

import com.example.booking.config.AppProperties;
import com.example.booking.domain.*;
import com.example.booking.dto.HoldDtos;
import com.example.booking.exception.ConflictException;
import com.example.booking.exception.ForbiddenException;
import com.example.booking.exception.NotFoundException;
import com.example.booking.repository.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeatHoldService {

    private static final Logger log = LoggerFactory.getLogger(SeatHoldService.class);

    private final SeatHoldRepository holdRepository;
    private final ShowSeatRepository showSeatRepository;
    private final ShowRepository showRepository;
    private final UserRepository userRepository;
    private final AppProperties props;

    public SeatHoldService(SeatHoldRepository holdRepository, ShowSeatRepository showSeatRepository,
                           ShowRepository showRepository, UserRepository userRepository, AppProperties props) {
        this.holdRepository = holdRepository;
        this.showSeatRepository = showSeatRepository;
        this.showRepository = showRepository;
        this.userRepository = userRepository;
        this.props = props;
    }

    @Transactional
    public HoldDtos.HoldResponse createHold(Long userId, HoldDtos.CreateHoldRequest req) {
        Show show = showRepository.findById(req.showId())
                .orElseThrow(() -> new NotFoundException("Show " + req.showId()));
        if (show.getStartTime().isBefore(Instant.now())) {
            throw new ConflictException("Show has already started");
        }
        User user = userRepository.getReferenceById(userId);

        Set<Long> distinctSeatIds = new HashSet<>(req.seatIds());
        if (distinctSeatIds.size() != req.seatIds().size()) {
            throw new ConflictException("Duplicate seat ids in request");
        }

        // Pessimistic-lock target ShowSeat rows in a deterministic id order to prevent deadlocks.
        List<ShowSeat> locked = showSeatRepository.lockByShowAndSeats(req.showId(), distinctSeatIds);
        if (locked.size() != distinctSeatIds.size()) {
            throw new NotFoundException("Some seats are not part of this show");
        }
        for (ShowSeat ss : locked) {
            if (ss.getStatus() != ShowSeatStatus.AVAILABLE) {
                throw new ConflictException("Seat " + ss.getSeat().getRowLabel() + ss.getSeat().getNumber()
                        + " is not available");
            }
        }

        SeatHold hold = SeatHold.builder()
                .user(user).show(show)
                .expiresAt(Instant.now().plusSeconds(props.hold().ttlSeconds()))
                .status(SeatHoldStatus.ACTIVE)
                .build();
        hold = holdRepository.save(hold);

        for (ShowSeat ss : locked) {
            ss.setStatus(ShowSeatStatus.HELD);
            ss.setHoldId(hold.getId());
        }
        showSeatRepository.saveAll(locked);

        return new HoldDtos.HoldResponse(hold.getId(), show.getId(),
                locked.stream().map(ShowSeat::getId).toList(),
                hold.getExpiresAt(), hold.getStatus().name());
    }

    @Transactional
    public void release(Long userId, Long holdId, boolean isAdmin) {
        SeatHold hold = holdRepository.findById(holdId)
                .orElseThrow(() -> new NotFoundException("Hold " + holdId));
        if (!isAdmin && !hold.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Cannot release another user's hold");
        }
        if (hold.getStatus() != SeatHoldStatus.ACTIVE) return;
        releaseInternal(hold, SeatHoldStatus.RELEASED);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void expireOne(Long holdId) {
        holdRepository.findById(holdId).ifPresent(h -> {
            if (h.getStatus() == SeatHoldStatus.ACTIVE) {
                releaseInternal(h, SeatHoldStatus.EXPIRED);
            }
        });
    }

    private void releaseInternal(SeatHold hold, SeatHoldStatus newStatus) {
        List<ShowSeat> seats = showSeatRepository.findAllByHoldId(hold.getId());
        for (ShowSeat ss : seats) {
            if (ss.getStatus() == ShowSeatStatus.HELD) {
                ss.setStatus(ShowSeatStatus.AVAILABLE);
                ss.setHoldId(null);
            }
        }
        showSeatRepository.saveAll(seats);
        hold.setStatus(newStatus);
        holdRepository.save(hold);
    }

    @Scheduled(fixedDelayString = "${app.hold.sweep-interval-ms}")
    public void sweepExpired() {
        List<SeatHold> expired = holdRepository.findAllByStatusAndExpiresAtBefore(
                SeatHoldStatus.ACTIVE, Instant.now());
        if (expired.isEmpty()) return;
        log.debug("Sweeping {} expired holds", expired.size());
        for (SeatHold h : expired) {
            try {
                expireOne(h.getId());
            } catch (Exception ex) {
                log.warn("Failed to expire hold {}: {}", h.getId(), ex.getMessage());
            }
        }
    }

    public long defaultTtlSeconds() {
        return props.hold().ttlSeconds();
    }

    @Transactional(readOnly = true)
    public SeatHold loadActive(Long holdId, Long userId, boolean isAdmin) {
        SeatHold hold = holdRepository.findById(holdId)
                .orElseThrow(() -> new NotFoundException("Hold " + holdId));
        if (!isAdmin && !hold.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Hold does not belong to current user");
        }
        if (hold.getStatus() != SeatHoldStatus.ACTIVE) {
            throw new ConflictException("Hold is not active (status=" + hold.getStatus() + ")");
        }
        if (hold.getExpiresAt().isBefore(Instant.now())) {
            throw new ConflictException("Hold has expired");
        }
        return hold;
    }

    public Instant defaultExpiry() {
        return Instant.now().plus(props.hold().ttlSeconds(), ChronoUnit.SECONDS);
    }
}
