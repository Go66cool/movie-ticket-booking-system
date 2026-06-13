package com.example.booking.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.booking.domain.RefundPolicy;
import com.example.booking.repository.RefundPolicyRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundPolicyServiceTest {

    @Mock RefundPolicyRepository repo;
    @InjectMocks RefundPolicyService service;

    @Test
    void picksMostFavorableMatchingPolicy() {
        when(repo.findAllByActiveTrueOrderByHoursBeforeShowDesc()).thenReturn(List.of(
                policy("24h full", 24, "100"),
                policy("4h half", 4, "50"),
                policy("0 nothing", 0, "0")
        ));
        Instant now = Instant.now();
        Instant showStart = now.plus(48, ChronoUnit.HOURS);
        var quote = service.computeRefund(showStart, now, new BigDecimal("500.00"));
        assertThat(quote.amount()).isEqualByComparingTo("500.00");
        assertThat(quote.policyApplied()).contains("24");
    }

    @Test
    void partialRefundWhenWithinShorterWindow() {
        when(repo.findAllByActiveTrueOrderByHoursBeforeShowDesc()).thenReturn(List.of(
                policy("24h full", 24, "100"),
                policy("4h half", 4, "50"),
                policy("0 nothing", 0, "0")
        ));
        Instant now = Instant.now();
        Instant showStart = now.plus(5, ChronoUnit.HOURS);
        var quote = service.computeRefund(showStart, now, new BigDecimal("500.00"));
        assertThat(quote.amount()).isEqualByComparingTo("250.00");
    }

    @Test
    void noRefundWhenNoPolicyMatches() {
        when(repo.findAllByActiveTrueOrderByHoursBeforeShowDesc()).thenReturn(List.of(
                policy("24h full", 24, "100")
        ));
        Instant now = Instant.now();
        Instant showStart = now.plus(1, ChronoUnit.HOURS);
        var quote = service.computeRefund(showStart, now, new BigDecimal("500.00"));
        assertThat(quote.amount()).isEqualByComparingTo("0.00");
    }

    private RefundPolicy policy(String name, int hours, String pct) {
        return RefundPolicy.builder().name(name).hoursBeforeShow(hours)
                .refundPercent(new BigDecimal(pct)).active(true).build();
    }
}
