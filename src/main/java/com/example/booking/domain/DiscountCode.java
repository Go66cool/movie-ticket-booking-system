package com.example.booking.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "discount_codes", indexes = @Index(name = "ux_discount_code", columnList = "code", unique = true))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DiscountCode {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "percent_off", precision = 5, scale = 2)
    private BigDecimal percentOff;

    @Column(name = "flat_off", precision = 12, scale = 2)
    private BigDecimal flatOff;

    @Column(name = "max_discount", precision = 12, scale = 2)
    private BigDecimal maxDiscount;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_to", nullable = false)
    private Instant validTo;

    @Column(name = "usage_limit", nullable = false)
    private int usageLimit;

    @Column(name = "used_count", nullable = false)
    private int usedCount;

    @Column(nullable = false)
    private boolean active;

    @Version
    private Long version;
}
