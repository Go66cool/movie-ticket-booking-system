package com.example.booking.repository;

import com.example.booking.domain.RefundPolicy;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundPolicyRepository extends JpaRepository<RefundPolicy, Long> {
    List<RefundPolicy> findAllByActiveTrueOrderByHoursBeforeShowDesc();
}
