package com.example.booking.web;

import com.example.booking.dto.HoldDtos;
import com.example.booking.service.SeatHoldService;
import com.example.booking.service.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/holds")
public class HoldController {

    private final SeatHoldService seatHoldService;

    public HoldController(SeatHoldService seatHoldService) {
        this.seatHoldService = seatHoldService;
    }

    @PostMapping
    public ResponseEntity<HoldDtos.HoldResponse> create(@Valid @RequestBody HoldDtos.CreateHoldRequest req) {
        return ResponseEntity.ok(seatHoldService.createHold(SecurityUtil.currentUserId(), req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> release(@PathVariable Long id) {
        seatHoldService.release(SecurityUtil.currentUserId(), id, SecurityUtil.hasRole("ADMIN"));
        return ResponseEntity.noContent().build();
    }
}
