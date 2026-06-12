package com.example.booking.service;

import com.example.booking.domain.PricingTier;
import com.example.booking.domain.SeatCategory;
import com.example.booking.domain.ShowType;
import com.example.booking.dto.PricingTierDtos;
import com.example.booking.repository.PricingTierRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PricingService {

    private final PricingTierRepository tierRepository;

    public PricingService(PricingTierRepository tierRepository) {
        this.tierRepository = tierRepository;
    }

    @Transactional
    public PricingTierDtos.PricingTierResponse create(PricingTierDtos.CreatePricingTierRequest r) {
        PricingTier t = tierRepository.save(PricingTier.builder()
                .name(r.name()).showType(r.showType()).seatCategory(r.seatCategory())
                .multiplier(r.multiplier()).build());
        return PricingTierDtos.PricingTierResponse.from(t);
    }

    @Transactional(readOnly = true)
    public List<PricingTierDtos.PricingTierResponse> list() {
        return tierRepository.findAll().stream().map(PricingTierDtos.PricingTierResponse::from).toList();
    }

    /**
     * Compute seat price = basePrice * mostSpecificMultiplier. Specificity ordering:
     * exact (showType + category) > showType only > category only > catch-all > 1.0.
     */
    public BigDecimal priceFor(BigDecimal basePrice, ShowType showType, SeatCategory category) {
        List<PricingTier> tiers = tierRepository.findAll();
        BigDecimal multiplier = tiers.stream()
                .filter(t -> matches(t, showType, category))
                .max(Comparator.comparingInt(PricingService::specificity))
                .map(PricingTier::getMultiplier)
                .orElse(BigDecimal.ONE);
        return basePrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    private static boolean matches(PricingTier t, ShowType st, SeatCategory cat) {
        return (t.getShowType() == null || t.getShowType() == st)
            && (t.getSeatCategory() == null || t.getSeatCategory() == cat);
    }

    private static int specificity(PricingTier t) {
        int s = 0;
        if (t.getShowType() != null) s += 2;
        if (t.getSeatCategory() != null) s += 1;
        return s;
    }
}
