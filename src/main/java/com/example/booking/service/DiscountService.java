package com.example.booking.service;

import com.example.booking.domain.DiscountCode;
import com.example.booking.dto.DiscountDtos;
import com.example.booking.exception.BadRequestException;
import com.example.booking.exception.NotFoundException;
import com.example.booking.repository.DiscountCodeRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DiscountService {

    private final DiscountCodeRepository repository;

    public DiscountService(DiscountCodeRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public DiscountDtos.DiscountResponse create(DiscountDtos.CreateDiscountRequest r) {
        if ((r.percentOff() == null || r.percentOff().signum() == 0)
            && (r.flatOff() == null || r.flatOff().signum() == 0)) {
            throw new BadRequestException("Either percentOff or flatOff must be > 0");
        }
        if (!r.validFrom().isBefore(r.validTo())) {
            throw new BadRequestException("validFrom must be before validTo");
        }
        DiscountCode d = repository.save(DiscountCode.builder()
                .code(r.code().toUpperCase())
                .percentOff(r.percentOff() == null ? BigDecimal.ZERO : r.percentOff())
                .flatOff(r.flatOff() == null ? BigDecimal.ZERO : r.flatOff())
                .maxDiscount(r.maxDiscount())
                .validFrom(r.validFrom()).validTo(r.validTo())
                .usageLimit(r.usageLimit()).usedCount(0).active(r.active())
                .build());
        return DiscountDtos.DiscountResponse.from(d);
    }

    @Transactional(readOnly = true)
    public List<DiscountDtos.DiscountResponse> list() {
        return repository.findAll().stream().map(DiscountDtos.DiscountResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public DiscountDtos.DiscountResponse get(Long id) {
        return DiscountDtos.DiscountResponse.from(repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Discount " + id)));
    }

    /**
     * Applies a discount code to {@code subtotal} and atomically increments its usedCount,
     * returning the discount amount (subtotal - finalAmount). Locks the row to prevent
     * over-use under concurrency. Returns Optional.empty() and zero discount if code is null/blank.
     */
    @Transactional
    public DiscountApplication apply(String rawCode, BigDecimal subtotal) {
        if (rawCode == null || rawCode.isBlank()) {
            return new DiscountApplication(null, BigDecimal.ZERO);
        }
        DiscountCode code = repository.lockByCode(rawCode.trim())
                .orElseThrow(() -> new BadRequestException("Invalid discount code"));
        Instant now = Instant.now();
        if (!code.isActive() || now.isBefore(code.getValidFrom()) || now.isAfter(code.getValidTo())) {
            throw new BadRequestException("Discount code is not active");
        }
        if (code.getUsedCount() >= code.getUsageLimit()) {
            throw new BadRequestException("Discount code usage limit reached");
        }
        BigDecimal discount = BigDecimal.ZERO;
        if (code.getPercentOff() != null && code.getPercentOff().signum() > 0) {
            discount = subtotal.multiply(code.getPercentOff())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        if (code.getFlatOff() != null && code.getFlatOff().signum() > 0) {
            discount = discount.add(code.getFlatOff());
        }
        if (code.getMaxDiscount() != null && discount.compareTo(code.getMaxDiscount()) > 0) {
            discount = code.getMaxDiscount();
        }
        if (discount.compareTo(subtotal) > 0) discount = subtotal;
        code.setUsedCount(code.getUsedCount() + 1);
        repository.save(code);
        return new DiscountApplication(code, discount.setScale(2, RoundingMode.HALF_UP));
    }

    /** Decrement usedCount when a booking is cancelled before payment completion. */
    @Transactional
    public void revert(Long discountId) {
        if (discountId == null) return;
        Optional<DiscountCode> opt = repository.findById(discountId);
        opt.ifPresent(c -> {
            if (c.getUsedCount() > 0) {
                c.setUsedCount(c.getUsedCount() - 1);
                repository.save(c);
            }
        });
    }

    public record DiscountApplication(DiscountCode code, BigDecimal discountAmount) {}
}
