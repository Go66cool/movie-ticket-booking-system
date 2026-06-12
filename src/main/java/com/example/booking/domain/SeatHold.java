package com.example.booking.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "seat_holds",
        indexes = {
            @Index(name = "ix_holds_user", columnList = "user_id"),
            @Index(name = "ix_holds_show", columnList = "show_id"),
            @Index(name = "ix_holds_status_exp", columnList = "status,expires_at")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SeatHold {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatHoldStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
