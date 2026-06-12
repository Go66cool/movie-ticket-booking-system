package com.example.booking.service;

import com.example.booking.domain.RefundPolicy;
import com.example.booking.dto.RefundPolicyDtos;
import com.example.booking.repository.RefundPolicyRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefundPolicyService {

    private final RefundPolicyRepository policyRepository;

    public RefundPolicyService(RefundPolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    @Transactional
    public RefundPolicyDtos.PolicyResponse create(RefundPolicyDtos.CreatePolicyRequest r) {
        RefundPolicy p = policyRepository.save(RefundPolicy.builder()
                .name(r.name()).hoursBeforeShow(r.hoursBeforeShow())
                .refundPercent(r.refundPercent()).active(r.active()).build());
        return RefundPolicyDtos.PolicyResponse.from(p);
    }

    @Transactional(readOnly = true)
    public List<RefundPolicyDtos.PolicyResponse> list() {
        return policyRepository.findAll().stream().map(RefundPolicyDtos.PolicyResponse::from).toList();
    }

    /**
     * Choose the matching active policy with the highest refundPercent (most favorable to customer).
     * A policy matches if (showStart - now) >= hoursBeforeShow.
     */
    public RefundQuote computeRefund(Instant showStart, Instant now, BigDecimal totalAmount) {
        long hoursToShow = Math.max(0, Duration.between(now, showStart).toHours());
        Optional<RefundPolicy> best = policyRepository.findAllByActiveTrueOrderByHoursBeforeShowDesc().stream()
                .filter(p -> hoursToShow >= p.getHoursBeforeShow())
                .max(Comparator.comparing(RefundPolicy::getRefundPercent));
        if (best.isEmpty()) {
            return new RefundQuote(BigDecimal.ZERO.setScale(2), "No refund (no matching policy)");
        }
        RefundPolicy p = best.get();
        BigDecimal amt = totalAmount.multiply(p.getRefundPercent())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return new RefundQuote(amt, p.getName() + " (" + p.getRefundPercent() + "% if ≥ "
                + p.getHoursBeforeShow() + "h before show)");
    }

    public record RefundQuote(BigDecimal amount, String policyApplied) {}
}
