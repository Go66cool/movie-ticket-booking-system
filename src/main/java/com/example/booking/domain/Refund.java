package com.example.booking.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "refunds", indexes = @Index(name = "ix_refunds_booking", columnList = "booking_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Refund {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "policy_applied", length = 200)
    private String policyApplied;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @PrePersist
    void onCreate() {
        if (processedAt == null) processedAt = Instant.now();
    }
}
