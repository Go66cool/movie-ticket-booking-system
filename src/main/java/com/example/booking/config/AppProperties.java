package com.example.booking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Security security, Hold hold, Notifications notifications, Seed seed) {
    public record Security(Jwt jwt) {
        public record Jwt(String secret, long ttlMinutes) {}
    }
    public record Hold(long ttlSeconds, long sweepIntervalMs) {}
    public record Notifications(boolean enabled) {}
    public record Seed(boolean enabled) {}
}
