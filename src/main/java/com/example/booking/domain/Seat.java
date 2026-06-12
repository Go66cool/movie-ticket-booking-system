package com.example.booking.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "seats",
        uniqueConstraints = @UniqueConstraint(columnNames = {"screen_id", "row_label", "number"}),
        indexes = @Index(name = "ix_seats_screen", columnList = "screen_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Seat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "screen_id", nullable = false)
    private Screen screen;

    @Column(name = "row_label", nullable = false, length = 5)
    private String rowLabel;

    @Column(nullable = false)
    private int number;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatCategory category;
}
