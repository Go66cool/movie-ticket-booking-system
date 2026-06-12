package com.example.booking.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "movies", indexes = @Index(name = "ix_movies_title", columnList = "title"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Movie {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 50)
    private String language;

    @Column(nullable = false, length = 100)
    private String genre;

    @Column(nullable = false, length = 10)
    private String rating;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(length = 2000)
    private String synopsis;
}
