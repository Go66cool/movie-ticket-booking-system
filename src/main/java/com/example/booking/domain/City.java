package com.example.booking.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cities", uniqueConstraints = @UniqueConstraint(columnNames = {"name", "state"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class City {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String state;
}
