package com.example.booking.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "theaters", indexes = @Index(name = "ix_theaters_city", columnList = "city_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Theater {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 400)
    private String address;
}
