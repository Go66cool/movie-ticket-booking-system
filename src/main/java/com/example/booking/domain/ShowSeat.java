package com.example.booking.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Entity
@Table(name = "show_seats",
        uniqueConstraints = @UniqueConstraint(columnNames = {"show_id", "seat_id"}),
        indexes = {
            @Index(name = "ix_show_seats_show", columnList = "show_id"),
            @Index(name = "ix_show_seats_hold", columnList = "hold_id")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ShowSeat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShowSeatStatus status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "hold_id")
    private Long holdId;

    @Version
    private Long version;
}
