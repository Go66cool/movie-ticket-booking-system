package com.example.booking.service;

import com.example.booking.domain.Booking;
import com.example.booking.domain.Payment;
import com.example.booking.domain.PaymentStatus;
import com.example.booking.repository.PaymentRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mock payment provider. {@code mockOutcome="FAIL"} forces a failure; otherwise succeeds.
 * A real implementation would wrap a PSP SDK and verify webhook signatures.
 */
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public Payment charge(Booking booking, String method, String mockOutcome) {
        boolean ok = mockOutcome == null || !mockOutcome.equalsIgnoreCase("FAIL");
        Payment p = Payment.builder()
                .booking(booking)
                .amount(booking.getTotalAmount())
                .provider(method == null ? "MOCK" : method.toUpperCase())
                .transactionId("TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase())
                .status(ok ? PaymentStatus.SUCCESS : PaymentStatus.FAILED)
                .completedAt(Instant.now())
                .build();
        return paymentRepository.save(p);
    }

    @Transactional
    public Payment refund(Booking booking) {
        Payment p = Payment.builder()
                .booking(booking)
                .amount(booking.getTotalAmount().negate())
                .provider("MOCK")
                .transactionId("REF-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase())
                .status(PaymentStatus.REFUNDED)
                .completedAt(Instant.now())
                .build();
        return paymentRepository.save(p);
    }
}
