package com.example.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.booking.domain.PricingTier;
import com.example.booking.domain.SeatCategory;
import com.example.booking.domain.ShowType;
import com.example.booking.repository.PricingTierRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

    @Mock PricingTierRepository tierRepository;
    @InjectMocks PricingService pricingService;

    @Test
    void picksMostSpecificTier() {
        when(tierRepository.findAll()).thenReturn(List.of(
                tier(null, null, "1.0"),
                tier(ShowType.WEEKEND, null, "1.2"),
                tier(null, SeatCategory.PREMIUM, "1.5"),
                tier(ShowType.WEEKEND, SeatCategory.PREMIUM, "2.0")
        ));
        BigDecimal price = pricingService.priceFor(new BigDecimal("100.00"), ShowType.WEEKEND, SeatCategory.PREMIUM);
        assertThat(price).isEqualByComparingTo("200.00");
    }

    @Test
    void fallsBackToOneWhenNoTierMatches() {
        when(tierRepository.findAll()).thenReturn(List.of());
        BigDecimal price = pricingService.priceFor(new BigDecimal("99.00"), ShowType.REGULAR, SeatCategory.REGULAR);
        assertThat(price).isEqualByComparingTo("99.00");
    }

    @Test
    void picksShowTypeOnlyOverCategoryOnly() {
        when(tierRepository.findAll()).thenReturn(List.of(
                tier(ShowType.PREMIERE, null, "1.5"),
                tier(null, SeatCategory.REGULAR, "1.2")
        ));
        BigDecimal price = pricingService.priceFor(new BigDecimal("100.00"), ShowType.PREMIERE, SeatCategory.REGULAR);
        assertThat(price).isEqualByComparingTo("150.00");
    }

    private PricingTier tier(ShowType st, SeatCategory cat, String mult) {
        return PricingTier.builder().name("t").showType(st).seatCategory(cat).multiplier(new BigDecimal(mult)).build();
    }
}
