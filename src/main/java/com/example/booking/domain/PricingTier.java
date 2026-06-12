package com.example.booking.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

/**
 * Multiplier applied on the show's base price for a given (showType, seatCategory) pair.
 * Both filters are optional — null means "matches any". The most specific match wins
 * (both matched > showType only > category only > catch-all).
 */
@Entity
@Table(name = "pricing_tiers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PricingTier {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "show_type", length = 20)
    private ShowType showType;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_category", length = 20)
    private SeatCategory seatCategory;

    @Column(nullable = false, precision = 6, scale = 3)
    private BigDecimal multiplier;
}
