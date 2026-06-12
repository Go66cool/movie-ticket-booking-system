package com.example.booking.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "screens",
        uniqueConstraints = @UniqueConstraint(columnNames = {"theater_id", "name"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Screen {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "theater_id", nullable = false)
    private Theater theater;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "total_seats", nullable = false)
    private int totalSeats;
}
