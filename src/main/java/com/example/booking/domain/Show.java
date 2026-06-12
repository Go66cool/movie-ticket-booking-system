package com.example.booking.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "shows",
        indexes = {
            @Index(name = "ix_shows_movie", columnList = "movie_id"),
            @Index(name = "ix_shows_screen_start", columnList = "screen_id,start_time")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Show {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "screen_id", nullable = false)
    private Screen screen;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "show_type", nullable = false, length = 20)
    private ShowType showType;
}
