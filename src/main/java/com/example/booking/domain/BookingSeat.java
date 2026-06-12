package com.example.booking.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Entity
@Table(name = "booking_seats",
        uniqueConstraints = @UniqueConstraint(columnNames = {"show_seat_id"}),
        indexes = @Index(name = "ix_booking_seats_booking", columnList = "booking_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BookingSeat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "show_seat_id", nullable = false)
    private ShowSeat showSeat;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;
}
