package com.example.booking.repository;

import com.example.booking.domain.PricingTier;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PricingTierRepository extends JpaRepository<PricingTier, Long> {
    List<PricingTier> findAll();
}
