package com.example.booking.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

/**
 * Refund policy rule: if cancellation happens at least {@code hoursBeforeShow} hours
 * before show start, refund {@code refundPercent}% of the total amount. Multiple
 * rules are evaluated and the most favorable matching one wins.
 */
@Entity
@Table(name = "refund_policies")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RefundPolicy {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "hours_before_show", nullable = false)
    private int hoursBeforeShow;

    @Column(name = "refund_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal refundPercent;

    @Column(nullable = false)
    private boolean active;
}
