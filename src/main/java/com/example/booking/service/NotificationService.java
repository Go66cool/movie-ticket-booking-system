package com.example.booking.service;

import com.example.booking.config.AppProperties;
import java.math.BigDecimal;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Lightweight pluggable notification publisher. In production this would push to a queue
 * (SQS / Kafka / SES). Here it logs asynchronously on a dedicated executor so the booking
 * flow is never blocked by notification delivery. Payloads are primitives so the async
 * thread never shares Hibernate session state with the caller.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final AppProperties props;

    public NotificationService(AppProperties props) {
        this.props = props;
    }

    @Async("notificationExecutor")
    public void sendBookingConfirmation(String toEmail, String reference, BigDecimal amount) {
        if (!props.notifications().enabled()) return;
        log.info("[NOTIFY] Booking CONFIRMED → to={} ref={} amount={}", toEmail, reference, amount);
    }

    @Async("notificationExecutor")
    public void sendBookingCancelled(String toEmail, String reference, BigDecimal refundAmount) {
        if (!props.notifications().enabled()) return;
        log.info("[NOTIFY] Booking CANCELLED → to={} ref={} refund={}", toEmail, reference, refundAmount);
    }

    @Async("notificationExecutor")
    public void sendReminder(String toEmail, String reference, Instant showStart) {
        if (!props.notifications().enabled()) return;
        log.info("[NOTIFY] Show reminder → to={} ref={} startsAt={}", toEmail, reference, showStart);
    }
}
